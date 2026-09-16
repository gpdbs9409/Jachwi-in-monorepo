from fastapi import FastAPI
from pydantic import BaseModel
from contextlib import asynccontextmanager
from typing import Optional

from app.embedder import embed, embed_batch, building_to_text, get_model
from app.vector_store import ensure_collection, search


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 서버 시작 시 Qdrant 컬렉션 확인/생성
    ensure_collection()
    # 임베딩 모델을 미리 로드 (첫 검색 요청이 모델 다운로드/로딩(~수 초~수십 초)을
    # 기다리다 Spring 쪽 RestTemplate readTimeout(10s)에 걸려 DB 폴백으로
    # 새는 것을 방지 — 콜드스타트 지연을 서버 기동 시점으로 옮긴다)
    get_model()
    yield


app = FastAPI(title="Jachwi-in Embedding Service", lifespan=lifespan)


# ─────────────────────────────────────────────
# Request / Response 모델
# ─────────────────────────────────────────────

class EmbedRequest(BaseModel):
    text: str

class EmbedResponse(BaseModel):
    vector: list[float]

class SearchRequest(BaseModel):
    query: str          # 사용자 조건 자연어 (예: "CCTV 많고 카페 가까운 곳")
    top_k: int = 20
    # 지도 뷰포트 등으로 위치를 제한할 때 사용 (선택)
    min_x: Optional[float] = None
    max_x: Optional[float] = None
    min_y: Optional[float] = None
    max_y: Optional[float] = None

class SearchResponse(BaseModel):
    results: list[dict]


# ─────────────────────────────────────────────
# 엔드포인트
# ─────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/embed", response_model=EmbedResponse)
def embed_text(req: EmbedRequest):
    """단일 텍스트 임베딩 (Spring Boot에서 필요시 호출)"""
    return EmbedResponse(vector=embed(req.text))


@app.post("/search", response_model=SearchResponse)
def search_buildings(req: SearchRequest):
    """사용자 조건 → (선택적 좌표범위 내) 유사 건물 검색"""
    vector = embed(req.query)
    results = search(
        vector,
        top_k=req.top_k,
        min_x=req.min_x,
        max_x=req.max_x,
        min_y=req.min_y,
        max_y=req.max_y,
    )
    return SearchResponse(results=results)
