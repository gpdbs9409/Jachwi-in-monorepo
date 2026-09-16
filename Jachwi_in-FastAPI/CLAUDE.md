# CLAUDE.md — Embedding Server (FastAPI :8000)

## 역할
한국어 임베딩 모델로 건물 데이터를 벡터화하고 유사 건물 검색 제공.
Main Server의 LLM 추천 기능이 이 서버를 호출함.

## API
```
GET  /health
POST /embed   {text: str} → {vector: list[float]}  # 768차원
POST /search  {query: str, top_k: int=20} → {results: [{score, building}]}
```

## 구성 요소
- `app/embedder.py`   : 모델 로드 (jhgan/ko-sroberta-multitask, ~400MB)
- `app/vector_store.py`: Qdrant 클라이언트 (컬렉션: buildings, 거리: COSINE)
- `app/schemas.py`    : Pydantic 스키마 (BuildingBase, SearchRequest/Response 등)
- `scripts/ingest.py` : MySQL → 임베딩 → Qdrant 적재 (최초 1회 실행 필요)

## 초기 데이터 적재
```bash
cp .env.example .env  # DB/Qdrant 연결 정보 설정
python -m scripts.ingest
```

## 환경변수
```
QDRANT_HOST=localhost (Docker: qdrant)
QDRANT_PORT=6333
DATABASE_URL=mysql+pymysql://root:root@localhost:3306/jachwi
```

## Qdrant payload 구조
한글 키로 저장됨: 시도명, 시군구, 법정읍면동명, 도로명, 편의점, 카페, CCTV, 버스정류장, 가로등, 병원, 식당, 학교_거리

## 주의
- 모델 최초 다운로드 시 ~400MB. Dockerfile에서 빌드 시 사전 다운로드함.
- Qdrant에 데이터 없으면 /search 결과가 빈 배열 → Main Server가 DB 폴백 사용
