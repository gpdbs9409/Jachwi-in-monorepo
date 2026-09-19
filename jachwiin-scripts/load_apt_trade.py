"""
국토교통부 아파트 매매 실거래가 → MySQL 적재 스크립트
사용법: python load_apt_trade.py --region 11680 --start 202301 --end 202312
"""

import os
import requests
import xml.etree.ElementTree as ET
import pymysql
import argparse
import time
from urllib.parse import unquote

# ── 설정 ──────────────────────────────────────────────
API_KEY = os.environ.get("MOLIT_API_KEY", "")
API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade"

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "password",
    "db": "jachwiin_db",
    "charset": "utf8mb4",
}

# 서울 주요 구 법정동 코드
SEOUL_REGIONS = {
    "종로구": "11110", "중구":   "11140", "용산구": "11170",
    "성동구": "11200", "광진구": "11215", "동대문구": "11230",
    "중랑구": "11260", "성북구": "11290", "강북구": "11305",
    "도봉구": "11320", "노원구": "11350", "은평구": "11380",
    "서대문구": "11410", "마포구": "11440", "양천구": "11470",
    "강서구": "11500", "구로구": "11530", "금천구": "11545",
    "영등포구": "11560", "동작구": "11590", "관악구": "11620",
    "서초구": "11650", "강남구": "11680", "송파구": "11710",
    "강동구": "11740",
}
# ──────────────────────────────────────────────────────


def create_table(conn):
    with conn.cursor() as cur:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS apt_trade (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                region_code   VARCHAR(10),
                dong          VARCHAR(50),
                apt_name      VARCHAR(100),
                area          DECIMAL(8,2)  COMMENT '전용면적(㎡)',
                floor         INT,
                built_year    INT,
                deal_year     INT,
                deal_month    INT,
                deal_day      VARCHAR(10),
                price         INT           COMMENT '거래금액(만원)',
                jibun         VARCHAR(20),
                created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_region (region_code),
                INDEX idx_apt   (apt_name),
                INDEX idx_deal  (deal_year, deal_month)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)
    conn.commit()
    print("✅ apt_trade 테이블 준비 완료")


def fetch_page(region_code, deal_ym, page_no, rows=100):
    params = {
        "serviceKey": API_KEY,
        "LAWD_CD":    region_code,
        "DEAL_YMD":   deal_ym,
        "pageNo":     page_no,
        "numOfRows":  rows,
    }
    resp = requests.get(API_URL, params=params, timeout=10)
    resp.raise_for_status()
    return ET.fromstring(resp.content)


def parse_items(root):
    items = []
    for item in root.findall(".//item"):
        def g(tag):
            el = item.find(tag)
            return el.text.strip() if el is not None and el.text else None

        price_raw = g("dealAmount")
        price = int(price_raw.replace(",", "").strip()) if price_raw else None

        items.append({
            "dong":       g("umdNm"),
            "apt_name":   g("aptNm"),
            "area":       g("excluUseAr"),
            "floor":      g("floor"),
            "built_year": g("buildYear"),
            "deal_year":  g("dealYear"),
            "deal_month": g("dealMonth"),
            "deal_day":   g("dealDay"),
            "price":      price,
            "jibun":      g("jibun"),
        })
    return items


def insert_items(conn, region_code, items):
    if not items:
        return 0
    sql = """
        INSERT INTO apt_trade
            (region_code, dong, apt_name, area, floor, built_year,
             deal_year, deal_month, deal_day, price, jibun)
        VALUES
            (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    rows = [
        (region_code, r["dong"], r["apt_name"], r["area"], r["floor"],
         r["built_year"], r["deal_year"], r["deal_month"], r["deal_day"],
         r["price"], r["jibun"])
        for r in items
    ]
    with conn.cursor() as cur:
        cur.executemany(sql, rows)
    conn.commit()
    return len(rows)


def load_region_month(conn, region_code, deal_ym):
    page, total_inserted = 1, 0
    while True:
        root = fetch_page(region_code, deal_ym, page)
        items = parse_items(root)
        if not items:
            break
        total_inserted += insert_items(conn, region_code, items)

        total_count_el = root.find(".//totalCount")
        total_count = int(total_count_el.text) if total_count_el is not None else 0
        if page * 100 >= total_count:
            break
        page += 1
        time.sleep(0.3)  # API 호출 간격

    return total_inserted


def ym_range(start, end):
    """'202301' ~ '202312' → ['202301', '202302', ..., '202312']"""
    months = []
    y, m = int(start[:4]), int(start[4:])
    ey, em = int(end[:4]), int(end[4:])
    while (y, m) <= (ey, em):
        months.append(f"{y}{m:02d}")
        m += 1
        if m > 12:
            m, y = 1, y + 1
    return months


def main():
    parser = argparse.ArgumentParser(description="아파트 매매 실거래가 적재")
    parser.add_argument("--region", default="all",
                        help="법정동 코드 (예: 11680) 또는 'all' (서울 전체)")
    parser.add_argument("--start",  default="202401", help="시작 년월 (YYYYMM)")
    parser.add_argument("--end",    default="202412", help="종료 년월 (YYYYMM)")
    args = parser.parse_args()
    if not API_KEY:
        parser.error("MOLIT_API_KEY 환경변수가 필요합니다")

    regions = SEOUL_REGIONS if args.region == "all" else {args.region: args.region}
    months  = ym_range(args.start, args.end)

    conn = pymysql.connect(**DB_CONFIG)
    create_table(conn)

    grand_total = 0
    for region_name, region_code in regions.items():
        for ym in months:
            try:
                cnt = load_region_month(conn, region_code, ym)
                print(f"  {region_name} {ym}: {cnt}건 적재")
                grand_total += cnt
                time.sleep(0.5)
            except Exception as e:
                print(f"  ⚠️  {region_name} {ym} 실패: {type(e).__name__}")

    conn.close()
    print(f"\n✅ 완료! 총 {grand_total}건 적재")


if __name__ == "__main__":
    main()
