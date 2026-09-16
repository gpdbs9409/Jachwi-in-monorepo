from sentence_transformers import SentenceTransformer
import numpy as np

# 한국어 특화 모델 - 최초 실행 시 자동 다운로드 (~400MB)
_model = None

def get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        _model = SentenceTransformer("jhgan/ko-sroberta-multitask")
    return _model


def embed(text: str) -> list[float]:
    vector = get_model().encode(text, normalize_embeddings=True)
    return vector.tolist()


def embed_batch(texts: list[str]) -> list[list[float]]:
    vectors = get_model().encode(texts, normalize_embeddings=True, batch_size=64, show_progress_bar=True)
    return vectors.tolist()


def building_to_text(building: dict) -> str:
    """Building 엔티티를 임베딩용 자연어 텍스트로 변환"""
    return (
        f"{building.get('시도명', '')} {building.get('시군구', '')} "
        f"{building.get('법정읍면동명', '')} {building.get('도로명', '')} "
        f"편의점 {building.get('편의점', 0)}개 "
        f"카페 {building.get('카페', 0)}개 "
        f"버스정류장 {building.get('버스정류장', 0)}개 "
        f"가로등 {building.get('가로등', 0)}개 "
        f"CCTV {building.get('CCTV', 0)}개 "
        f"병원 {building.get('병원', 0)}개 "
        f"식당 {building.get('식당', 0)}개 "
        f"학교거리 {building.get('학교_거리', 0):.0f}m"
    )
