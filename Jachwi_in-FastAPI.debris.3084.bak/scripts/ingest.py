"""
건물 데이터 수집 스크립트
MySQL → 임베딩 → Qdrant 저장

실행: python -m scripts.ingest
"""

import os
import sys
from dotenv import load_dotenv
from sqlalchemy import create_engine, text

load_dotenv()

sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from app.embedder import embed_batch, building_to_text
from app.vector_store import ensure_collection, upsert_buildings

DB_URL = os.getenv(
    "DATABASE_URL",
    "mysql+pymysql://root:root@localhost:3306/jachwi?charset=utf8mb4"
)

BATCH_SIZE = 500  # 한 번에 처리할 건물 수


def fetch_buildings(engine, offset: int, limit: int) -> list[dict]:
    query = text("""
        SELECT id, x, y, 시도명, 시군구, 법정읍면동명, 도로명,
               건물본번, 건물부번, 편의점, 카페, 버스정류장,
               가로등, CCTV, 병원, 식당, 학교_거리
        FROM building
        WHERE x IS NOT NULL AND y IS NOT NULL
        ORDER BY id
        LIMIT :limit OFFSET :offset
    """)
    with engine.connect() as conn:
        rows = conn.execute(query, {"limit": limit, "offset": offset}).mappings().all()
    return [dict(r) for r in rows]


def run():
    ensure_collection()
    engine = create_engine(DB_URL)

    offset = 0
    total = 0

    print("[ingest] 시작")

    while True:
        buildings = fetch_buildings(engine, offset, BATCH_SIZE)
        if not buildings:
            break

        # 텍스트 변환
        texts = [building_to_text(b) for b in buildings]

        # 배치 임베딩
        vectors = embed_batch(texts)

        # Qdrant 저장 — point id는 MySQL의 실제 building.id 사용
        # (재실행/증분 반영 시에도 안정적으로 같은 건물을 덮어쓰기 위함)
        points = [
            {
                "id": buildings[i]["id"],
                "vector": vectors[i],
                "payload": buildings[i],
            }
            for i in range(len(buildings))
        ]
        upsert_buildings(points)

        total += len(buildings)
        offset += BATCH_SIZE
        print(f"[ingest] {total}개 처리 완료")

    print(f"[ingest] 완료 — 총 {total}개 건물 저장")


if __name__ == "__main__":
    run()
