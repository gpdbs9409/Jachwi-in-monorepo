from pydantic import BaseModel
from typing import Optional


# ─────────────────────────────────────────────
# Building
# ─────────────────────────────────────────────

class BuildingBase(BaseModel):
    id: Optional[int] = None
    x: float
    y: float
    province: Optional[str] = None       # 시도명
    district: Optional[str] = None       # 시군구
    neighborhood: Optional[str] = None   # 법정읍면동명
    street_name: Optional[str] = None    # 도로명
    bus_stop: int = 0                     # 버스정류장
    convenience_store: int = 0           # 편의점
    cafe: int = 0                        # 카페
    street_light: int = 0               # 가로등
    cctv: int = 0                        # CCTV
    hospital: int = 0                    # 병원
    restaurant: int = 0                  # 식당
    school_distance: float = 0.0        # 학교_거리 (m)

    class Config:
        from_attributes = True


# ─────────────────────────────────────────────
# Embed
# ─────────────────────────────────────────────

class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    vector: list[float]


# ─────────────────────────────────────────────
# Search
# ─────────────────────────────────────────────

class SearchRequest(BaseModel):
    query: str
    top_k: int = 20
    # 좌표 범위로 필터링 (선택)
    min_x: Optional[float] = None
    max_x: Optional[float] = None
    min_y: Optional[float] = None
    max_y: Optional[float] = None


class SearchResult(BaseModel):
    score: float
    building: BuildingBase


class SearchResponse(BaseModel):
    results: list[SearchResult]


# ─────────────────────────────────────────────
# Ingest
# ─────────────────────────────────────────────

class IngestResponse(BaseModel):
    indexed: int
    message: str
