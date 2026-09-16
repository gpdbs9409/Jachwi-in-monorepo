"""
아파트 매매 실거래가 CSV → MySQL 적재 스크립트
사용법: python3 load_apt_csv.py --file ~/Downloads/아파트\(매매\)_실거래가_*.csv
"""

import csv
import pymysql
import argparse
import os

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "password",
    "db": "jachwiin_db",
    "charset": "utf8mb4",
}

HEADER_ROW = "NO"  # 실제 컬럼 헤더가 시작하는 행 식별자


def create_table(conn):
    with conn.cursor() as cur:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS apt_trade (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                sigungu       VARCHAR(100)  COMMENT '시군구',
                jibun         VARCHAR(50)   COMMENT '번지',
                bon_bun       VARCHAR(10),
                bu_bun        VARCHAR(10),
                apt_name      VARCHAR(100)  COMMENT '단지명',
                area          DECIMAL(10,4) COMMENT '전용면적(㎡)',
                deal_ym       VARCHAR(6)    COMMENT '계약년월(YYYYMM)',
                deal_day      VARCHAR(5)    COMMENT '계약일',
                price         INT           COMMENT '거래금액(만원)',
                dong          VARCHAR(50)   COMMENT '동',
                floor         VARCHAR(10)   COMMENT '층',
                buyer         VARCHAR(20)   COMMENT '매수자',
                seller        VARCHAR(20)   COMMENT '매도자',
                built_year    INT           COMMENT '건축년도',
                road_addr     VARCHAR(200)  COMMENT '도로명',
                cancel_date   VARCHAR(20)   COMMENT '해제사유발생일',
                deal_type     VARCHAR(20)   COMMENT '거래유형',
                agent_region  VARCHAR(100)  COMMENT '중개사소재지',
                reg_date      VARCHAR(20)   COMMENT '등기일자',
                created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_sigungu (sigungu(20)),
                INDEX idx_apt    (apt_name(30)),
                INDEX idx_deal   (deal_ym)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)
    conn.commit()
    print("✅ apt_trade 테이블 준비 완료")


def find_header_line(filepath):
    """헤더 행 번호 반환 (NO, 시군구, ... 로 시작하는 행)"""
    with open(filepath, encoding="euc-kr", errors="replace") as f:
        for i, line in enumerate(f):
            if line.strip().startswith(f'"{HEADER_ROW}"') or line.strip().startswith(HEADER_ROW):
                return i
    raise ValueError("헤더 행을 찾을 수 없습니다.")


def parse_price(val):
    if not val or val.strip() in ("-", ""):
        return None
    try:
        return int(val.replace(",", "").strip())
    except ValueError:
        return None


def parse_int(val):
    if not val or val.strip() in ("-", ""):
        return None
    try:
        return int(val.strip())
    except ValueError:
        return None


def load_csv(conn, filepath):
    header_line = find_header_line(filepath)
    print(f"  헤더 위치: {header_line}행")

    inserted = 0
    batch = []
    BATCH_SIZE = 500

    sql = """
        INSERT INTO apt_trade
            (sigungu, jibun, bon_bun, bu_bun, apt_name, area,
             deal_ym, deal_day, price, dong, floor, buyer, seller,
             built_year, road_addr, cancel_date, deal_type, agent_region, reg_date)
        VALUES
            (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
    """

    with open(filepath, encoding="euc-kr", errors="replace", newline="") as f:
        for _ in range(header_line):
            next(f)
        reader = csv.DictReader(f)
        for row in reader:
            if not row.get("NO", "").strip().isdigit():
                continue
            batch.append((
                row.get("시군구", "").strip(),
                row.get("번지", "").strip(),
                row.get("본번", "").strip(),
                row.get("부번", "").strip(),
                row.get("단지명", "").strip(),
                row.get("전용면적(㎡)", "").strip() or None,
                row.get("계약년월", "").strip(),
                row.get("계약일", "").strip(),
                parse_price(row.get("거래금액(만원)", "")),
                row.get("동", "").strip(),
                row.get("층", "").strip(),
                row.get("매수자", "").strip(),
                row.get("매도자", "").strip(),
                parse_int(row.get("건축년도", "")),
                row.get("도로명", "").strip(),
                row.get("해제사유발생일", "").strip(),
                row.get("거래유형", "").strip(),
                row.get("중개사소재지", "").strip(),
                row.get("등기일자", "").strip(),
            ))

            if len(batch) >= BATCH_SIZE:
                with conn.cursor() as cur:
                    cur.executemany(sql, batch)
                conn.commit()
                inserted += len(batch)
                print(f"  {inserted}건 적재 중...")
                batch = []

    if batch:
        with conn.cursor() as cur:
            cur.executemany(sql, batch)
        conn.commit()
        inserted += len(batch)

    return inserted


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", required=True, help="CSV 파일 경로")
    args = parser.parse_args()

    filepath = os.path.expanduser(args.file)
    if not os.path.exists(filepath):
        print(f"❌ 파일 없음: {filepath}")
        return

    print(f"📂 파일: {filepath}")
    conn = pymysql.connect(**DB_CONFIG)
    create_table(conn)

    total = load_csv(conn, filepath)
    conn.close()
    print(f"\n✅ 완료! 총 {total}건 적재")


if __name__ == "__main__":
    main()
