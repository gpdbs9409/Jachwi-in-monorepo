from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance, VectorParams, PointStruct, Filter,
    FieldCondition, Range, PayloadSchemaType
)
import os

COLLECTION_NAME = "buildings"
VECTOR_SIZE = 768  # jhgan/ko-sroberta-multitask 출력 차원

_client = None

def get_client() -> QdrantClient:
    global _client
    if _client is None:
        _client = QdrantClient(
            host=os.getenv("QDRANT_HOST", "localhost"),
            port=int(os.getenv("QDRANT_PORT", 6333)),
        )
    return _client


def ensure_collection():
    client = get_client()
    existing = [c.name for c in client.get_collections().collections]
    if COLLECTION_NAME not in existing:
        client.create_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=VectorParams(size=VECTOR_SIZE, distance=Distance.COSINE),
        )
        print(f"[Qdrant] 컬렉션 '{COLLECTION_NAME}' 생성 완료")

    # x, y 좌표에 payload 인덱스 생성 (지오 range 필터 성능/정확도 확보)
    # 이미 존재해도 예외 없이 무시됨 (idempotent)
    try:
        client.create_payload_index(
            collection_name=COLLECTION_NAME,
            field_name="x",
            field_schema=PayloadSchemaType.FLOAT,
        )
        client.create_payload_index(
            collection_name=COLLECTION_NAME,
            field_name="y",
            field_schema=PayloadSchemaType.FLOAT,
        )
    except Exception as e:
        print(f"[Qdrant] payload 인덱스 생성 스킵: {e}")


def upsert_buildings(points: list[dict]):
    """건물 데이터를 Qdrant에 저장
    points: [{"id": int, "vector": [...], "payload": {building 필드}}]
    """
    client = get_client()
    client.upsert(
        collection_name=COLLECTION_NAME,
        points=[
            PointStruct(
                id=p["id"],
                vector=p["vector"],
                payload=p["payload"],
            )
            for p in points
        ],
    )


def _build_geo_filter(min_x, max_x, min_y, max_y) -> Filter | None:
    """지도 뷰포트(경도/위도 범위)로 후보를 제한하는 Qdrant Filter 생성.

    의미상 중요한 이유: 임베딩(문장 유사도)만으로는 "학교/지도에서 보고 있는 영역"
    같은 위치 조건을 정확히 걸러낼 수 없다 (건물 텍스트에 좌표가 아니라 행정동
    주소 문자열만 들어가기 때문). 그래서 위치는 Qdrant의 range filter로 정확히
    강제하고, 벡터 유사도는 그 안에서 "선호시설/분위기" 같은 정성적 조건을
    좁히는 데에만 쓴다.
    """
    if None in (min_x, max_x, min_y, max_y):
        return None
    return Filter(
        must=[
            FieldCondition(key="x", range=Range(gte=min_x, lte=max_x)),
            FieldCondition(key="y", range=Range(gte=min_y, lte=max_y)),
        ]
    )


def search(
    vector: list[float],
    top_k: int = 20,
    min_x: float | None = None,
    max_x: float | None = None,
    min_y: float | None = None,
    max_y: float | None = None,
) -> list[dict]:
    """유사도 검색 → payload(건물 정보) + score 반환.

    min_x/max_x/min_y/max_y가 주어지면 해당 좌표 범위 안의 건물만 후보로 삼는다.
    """
    client = get_client()
    query_filter = _build_geo_filter(min_x, max_x, min_y, max_y)
    results = client.search(
        collection_name=COLLECTION_NAME,
        query_vector=vector,
        query_filter=query_filter,
        limit=top_k,
        with_payload=True,
    )
    return [
        {"score": r.score, "building": r.payload}
        for r in results
    ]
