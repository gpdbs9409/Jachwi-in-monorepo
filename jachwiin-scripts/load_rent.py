"""MOLIT rental snapshots -> separate raw tables and a normalized UNION ALL view.

Credentials: MOLIT_API_KEY (encoded or decoded), DB_HOST/PORT/USER/PASSWORD/NAME.
Fetch completes to an atomic local cache before each region/month transaction.
Identical disclosed contracts are preserved using an occurrence number.
"""
import argparse
from collections import Counter
from datetime import date, datetime, timezone
from decimal import Decimal
import hashlib
import json
import os
from pathlib import Path
import re
import time
from urllib.parse import unquote
import xml.etree.ElementTree as ET

import pymysql
import requests

SOURCES = {
    "multi_family": ("RTMSDataSvcRHRent", "raw_multi_family_rent", "MULTI_FAMILY", "mhouseNm"),
    "officetel": ("RTMSDataSvcOffiRent", "raw_officetel_rent", "OFFICETEL", "offiNm"),
    "apt": ("RTMSDataSvcAptRent", "raw_apt_rent", "APT", "aptNm"),
}
REGIONS = {"11110", "11140", "11170", "11200", "11215", "11230", "11260", "11290",
           "11305", "11320", "11350", "11380", "11410", "11440", "11470", "11500",
           "11530", "11545", "11560", "11590", "11620", "11650", "11680", "11710", "11740"}


class ImportFailure(Exception):
    """Safe diagnostic: no credentials, request URL, or response body."""


def months_between(start, end):
    def parse(value):
        if not re.fullmatch(r"\d{6}", value):
            raise ValueError("Months must use YYYYMM")
        return date(int(value[:4]), int(value[4:]), 1)
    first, last = parse(start), parse(end)
    if first > last:
        raise ValueError("Start month must not follow end month")
    months = []
    while first <= last:
        months.append(first.strftime("%Y%m"))
        first = date(first.year + (first.month == 12), first.month % 12 + 1, 1)
    return months


def fetch_page(session, source, key, region, month, page, rows=1000):
    service = SOURCES[source][0]
    for attempt in range(4):
        try:
            response = session.get(
                f"https://apis.data.go.kr/1613000/{service}/get{service}",
                params={"serviceKey": unquote(key), "LAWD_CD": region, "DEAL_YMD": month,
                        "pageNo": page, "numOfRows": rows}, timeout=(10, 45))
        except requests.RequestException:
            if attempt == 3:
                raise ImportFailure("API connection failed after retries") from None
            time.sleep(2 ** attempt)
            continue
        if response.status_code == 429 or response.status_code >= 500:
            if attempt < 3:
                time.sleep(2 ** attempt)
                continue
        if response.status_code != 200:
            raise ImportFailure(f"API HTTP status {response.status_code}")
        try:
            root = ET.fromstring(response.content)
        except ET.ParseError:
            raise ImportFailure("API returned invalid XML") from None
        code = root.findtext(".//resultCode", "").strip()
        if code not in {"000", "00", "0"}:
            safe_code = code if re.fullmatch(r"[0-9]{1,4}", code) else "unavailable"
            raise ImportFailure(f"API result code {safe_code}")
        try:
            total = int(root.findtext(".//totalCount"))
            actual_page = int(root.findtext(".//pageNo"))
        except (TypeError, ValueError):
            raise ImportFailure("API pagination metadata missing") from None
        if total < 0 or actual_page != page:
            raise ImportFailure("API pagination metadata inconsistent")
        items = [{el.tag: (el.text or "").strip() for el in item} for item in root.findall(".//item")]
        return total, items
    raise ImportFailure("API retries exhausted")


def canonical(item):
    return json.dumps(item, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def download(session, source, key, region, month):
    items, expected, page, page_hashes = [], None, 1, set()
    while page <= 10000:
        total, batch = fetch_page(session, source, key, region, month, page)
        if expected is not None and total != expected:
            raise ImportFailure("API count changed during pagination; retry this month")
        expected = total
        if not batch and len(items) != total:
            raise ImportFailure("API returned an incomplete page sequence")
        fingerprint = hashlib.sha256(canonical(batch).encode()).hexdigest()
        if batch and fingerprint in page_hashes:
            raise ImportFailure("API repeated a page; refusing incomplete snapshot")
        page_hashes.add(fingerprint)
        items.extend(batch)
        if len(items) == total:
            return {"source": source, "region": region, "month": month,
                    "fetched_at": datetime.now(timezone.utc).isoformat(), "total": total, "items": items}
        if len(items) > total:
            raise ImportFailure("API item count exceeds totalCount")
        page += 1
        time.sleep(0.2)
    raise ImportFailure("API page limit exceeded")


def number(value, integral=False, required=False):
    value = str(value or "").replace(",", "").strip()
    if value in {"", "-"}:
        if required:
            raise ValueError("Required numeric value missing")
        return None
    result = Decimal(value)
    if not result.is_finite() or (integral and result != result.to_integral_value()):
        raise ValueError("Invalid numeric value")
    return int(result) if integral else result


def normalize(snapshot):
    source, region, month = snapshot["source"], snapshot["region"], snapshot["month"]
    _, _, building_type, name_field = SOURCES[source]
    if snapshot["total"] != len(snapshot["items"]):
        raise ValueError("Snapshot count mismatch")
    occurrences, rows = Counter(), []
    for item in snapshot["items"]:
        raw = canonical(item)
        digest = hashlib.sha256(raw.encode()).hexdigest()
        occurrences[digest] += 1
        deal_date = date(int(item["dealYear"]), int(item["dealMonth"]), int(item["dealDay"]))
        if deal_date.strftime("%Y%m") != month or item.get("sggCd") != region:
            raise ValueError("API item is outside requested region/month")
        deposit = number(item.get("deposit"), integral=True, required=True)
        rent = number(item.get("monthlyRent"), integral=True, required=True)
        if deposit < 0 or rent < 0:
            raise ValueError("Negative rental price")
        rows.append((region, month, digest, occurrences[digest], building_type,
                     "JEONSE" if rent == 0 else "MONTHLY_RENT", item.get("umdNm") or None,
                     item.get(name_field) or None, item.get("jibun") or None,
                     number(item.get("excluUseAr")), item.get("floor") or None,
                     number(item.get("buildYear"), integral=True), deal_date, deposit, rent,
                     item.get("contractType") or None, item.get("contractTerm") or None, raw,
                     datetime.fromisoformat(snapshot["fetched_at"]).astimezone(timezone.utc).replace(tzinfo=None)))
    return rows


def ensure_schema(conn):
    with conn.cursor() as cur:
        for _, table, _, _ in SOURCES.values():
            cur.execute(f"""CREATE TABLE IF NOT EXISTS {table} (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                region_code CHAR(5) NOT NULL, deal_ym CHAR(6) NOT NULL,
                record_hash CHAR(64) NOT NULL, occurrence_no INT NOT NULL,
                building_type VARCHAR(30) NOT NULL, trade_type VARCHAR(20) NOT NULL,
                dong VARCHAR(100), building_name VARCHAR(255), jibun VARCHAR(100),
                area DECIMAL(14,4), floor VARCHAR(30), built_year INT,
                deal_date DATE NOT NULL, deposit BIGINT NOT NULL, monthly_rent BIGINT NOT NULL,
                contract_type VARCHAR(100), contract_term VARCHAR(100), raw_payload JSON NOT NULL,
                fetched_at DATETIME(6) NOT NULL,
                UNIQUE KEY uk_snapshot_record(region_code,deal_ym,record_hash,occurrence_no),
                INDEX idx_rent_region_date(region_code,deal_date),
                INDEX idx_rent_type(trade_type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""")
        cur.execute("""CREATE TABLE IF NOT EXISTS rental_import_log (
            source VARCHAR(30) NOT NULL, region_code CHAR(5) NOT NULL, deal_ym CHAR(6) NOT NULL,
            row_count INT NOT NULL, fetched_at DATETIME(6) NOT NULL,
            PRIMARY KEY(source,region_code,deal_ym)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""")
        selects = []
        for source, (_, table, _, _) in SOURCES.items():
            selects.append(f"""SELECT '{source}' AS source, id AS source_row_id, region_code, deal_ym,
                building_type, trade_type, dong, building_name, jibun, area, floor, built_year,
                deal_date, deposit, monthly_rent, contract_type, contract_term, fetched_at FROM {table}""")
        cur.execute("CREATE OR REPLACE VIEW rental_trade AS " + " UNION ALL ".join(selects))
    conn.commit()


def import_snapshot(conn, snapshot):
    rows = normalize(snapshot)  # Validate everything BEFORE touching existing data.
    source, region, month = snapshot["source"], snapshot["region"], snapshot["month"]
    table = SOURCES[source][1]
    lock = f"jachwi:rent:{source}:{region}:{month}"
    with conn.cursor() as cur:
        cur.execute("SELECT GET_LOCK(%s, 10)", (lock,))
        if cur.fetchone()[0] != 1:
            raise ImportFailure("Another import owns this region/month")
    try:
        conn.begin()
        with conn.cursor() as cur:
            cur.execute(f"SELECT COUNT(*) FROM {table} WHERE region_code=%s AND deal_ym=%s", (region, month))
            previous = cur.fetchone()[0]
            # These are exclusively owned raw snapshot tables, not application history/listing tables.
            cur.execute(f"DELETE FROM {table} WHERE region_code=%s AND deal_ym=%s", (region, month))
            if rows:
                cur.executemany(f"""INSERT INTO {table}
                    (region_code,deal_ym,record_hash,occurrence_no,building_type,trade_type,dong,
                     building_name,jibun,area,floor,built_year,deal_date,deposit,monthly_rent,
                     contract_type,contract_term,raw_payload,fetched_at)
                    VALUES ({','.join(['%s'] * 19)})""", rows)
            fetched = datetime.fromisoformat(snapshot["fetched_at"]).astimezone(timezone.utc).replace(tzinfo=None)
            cur.execute("""INSERT INTO rental_import_log(source,region_code,deal_ym,row_count,fetched_at)
                VALUES (%s,%s,%s,%s,%s) ON DUPLICATE KEY UPDATE row_count=VALUES(row_count),fetched_at=VALUES(fetched_at)""",
                        (source,region,month,len(rows),fetched))
        conn.commit()
        return previous, len(rows)
    except Exception:
        conn.rollback()
        raise
    finally:
        with conn.cursor() as cur:
            cur.execute("SELECT RELEASE_LOCK(%s)", (lock,))


def run(args):
    months = months_between(args.start, args.end)
    regions = sorted(REGIONS) if args.regions == "seoul" else args.regions.split(",")
    if not regions or any(not re.fullmatch(r"\d{5}", r) for r in regions) or len(set(regions)) != len(regions):
        raise ValueError("Use distinct five-digit region codes or seoul")
    sources = list(SOURCES) if args.sources == "all" else args.sources.split(",")
    if any(s not in SOURCES for s in sources):
        raise ValueError("Unknown source")
    key = os.environ.get("MOLIT_API_KEY", "")
    if not key and not args.offline:
        raise ValueError("MOLIT_API_KEY is required")
    conn = None
    if not args.download_only:
        conn = pymysql.connect(host=os.environ.get("DB_HOST", "127.0.0.1"),
                               port=int(os.environ.get("DB_PORT", "3307")),
                               user=os.environ.get("DB_USER", "root"), password=os.environ.get("DB_PASSWORD", ""),
                               database=os.environ.get("DB_NAME", "jachwiin_db"), charset="utf8mb4", autocommit=False)
        ensure_schema(conn)
    results, failures = [], []
    try:
        with requests.Session() as session:
            for source in sources:
                for region in regions:
                    for month in months:
                        path = Path(args.cache_dir) / source / region / f"{month}.json"
                        try:
                            if args.offline:
                                snapshot = json.loads(path.read_text())
                            else:
                                snapshot = download(session,source,key,region,month)
                            if any(snapshot[k] != v for k,v in [("source",source),("region",region),("month",month)]):
                                raise ValueError("Cache identity mismatch")
                            normalize(snapshot)
                            if not args.offline:
                                path.parent.mkdir(parents=True,exist_ok=True)
                                tmp = path.with_suffix('.json.part')
                                tmp.write_text(json.dumps(snapshot,ensure_ascii=False))
                                tmp.replace(path)
                            previous, count = import_snapshot(conn,snapshot) if conn else (None,snapshot["total"])
                            result={"source":source,"region":region,"month":month,"rows":count,"previous_rows":previous}
                            results.append(result)
                            print(json.dumps(result),flush=True)
                        except Exception as exc:
                            # Do not stringify requests/DB exceptions: they may contain credentials or raw URLs.
                            failure={"source":source,"region":region,"month":month,"error":type(exc).__name__}
                            if isinstance(exc,ImportFailure):failure["detail"]=str(exc)
                            failures.append(failure)
                            print(json.dumps(failure),flush=True)
                        if not args.offline:time.sleep(0.25)
    finally:
        if conn:conn.close()
    report={"completed":results,"failed":failures,"total_rows":sum(r["rows"] for r in results),
            "mode":"download" if args.download_only else "database", "finished_at":datetime.now(timezone.utc).isoformat()}
    report_path=Path(args.report);report_path.parent.mkdir(parents=True,exist_ok=True)
    report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2))
    return 1 if failures else 0


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--regions',required=True,help='Comma-separated LAWD_CD codes or seoul')
    parser.add_argument('--start',required=True)
    parser.add_argument('--end',required=True)
    parser.add_argument('--sources',default='all',help='all or multi_family,officetel,apt')
    parser.add_argument('--cache-dir',required=True)
    parser.add_argument('--report',required=True)
    parser.add_argument('--download-only',action='store_true')
    parser.add_argument('--offline',action='store_true',help='Import previously validated cached snapshots without API requests')
    args=parser.parse_args()
    try:
        return run(args)
    except Exception as exc:
        print(json.dumps({'error':type(exc).__name__,'detail':'Import could not start; check configuration (secrets suppressed)'}))
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
