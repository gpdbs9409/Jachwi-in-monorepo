"""
임베딩 모델 벤치마크

비교 항목:
  1. 속도     - 인코딩 시간 (ms/건)
  2. 메모리   - 모델 로딩 후 RAM 사용량
  3. 검색품질 - 쿼리 → 건물 유사도 순위가 기대 순위와 얼마나 일치하는지

실행: python benchmark.py
"""

import time
import tracemalloc
import json
from pathlib import Path

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from sentence_transformers import SentenceTransformer
from tabulate import tabulate
from tqdm import tqdm

from data.queries import QUERIES, SAMPLE_BUILDINGS

# ─────────────────────────────────────────────
# 비교할 모델 목록
# ─────────────────────────────────────────────
MODELS = [
    {
        "name": "ko-sroberta",
        "model_id": "jhgan/ko-sroberta-multitask",
        "desc": "한국어 특화 Sentence-BERT",
    },
    {
        "name": "multilingual-mpnet",
        "model_id": "paraphrase-multilingual-mpnet-base-v2",
        "desc": "다국어 지원 (50개국어)",
    },
    {
        "name": "KoSimCSE-roberta",
        "model_id": "BM-K/KoSimCSE-roberta",
        "desc": "한국어 SimCSE (대조 학습)",
    },
    {
        "name": "multilingual-MiniLM",
        "model_id": "paraphrase-multilingual-MiniLM-L12-v2",
        "desc": "경량 다국어 모델 (빠름)",
    },
]

RESULTS_DIR = Path("results")
RESULTS_DIR.mkdir(exist_ok=True)


# ─────────────────────────────────────────────
# 1. 속도 측정
# ─────────────────────────────────────────────
def measure_speed(model: SentenceTransformer, texts: list[str], repeat: int = 3) -> float:
    """평균 인코딩 시간 (ms/건)"""
    times = []
    for _ in range(repeat):
        start = time.perf_counter()
        model.encode(texts, batch_size=64, show_progress_bar=False)
        elapsed = time.perf_counter() - start
        times.append(elapsed)
    avg_ms = (sum(times) / repeat) / len(texts) * 1000
    return round(avg_ms, 3)


# ─────────────────────────────────────────────
# 2. 메모리 측정
# ─────────────────────────────────────────────
def measure_memory(model_id: str) -> float:
    """모델 로딩 시 메모리 증가량 (MB)"""
    tracemalloc.start()
    _ = SentenceTransformer(model_id)
    current, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    return round(peak / 1024 / 1024, 1)


# ─────────────────────────────────────────────
# 3. 검색 품질 측정 (MRR — Mean Reciprocal Rank)
# ─────────────────────────────────────────────
def measure_retrieval_quality(model: SentenceTransformer) -> float:
    """
    각 쿼리에 대해 가장 기대에 맞는 건물이 top-K 안에 들어오는지 측정.
    기대 건물: expected 조건에 가장 부합하는 샘플 건물 (label로 매핑)
    단순화: 레이블이 '안전'인 쿼리는 'CCTV 높은' 건물이 1위여야 함
    → MRR(Mean Reciprocal Rank) 반환
    """
    building_texts = [b["desc"] for b in SAMPLE_BUILDINGS]
    building_vecs = model.encode(building_texts, normalize_embeddings=True)

    reciprocal_ranks = []

    for q in QUERIES:
        query_vec = model.encode(q["query"], normalize_embeddings=True)
        sims = cosine_similarity([query_vec], building_vecs)[0]
        ranked_ids = np.argsort(sims)[::-1]  # 높은 순

        # 기대 건물: expected 조건 기준으로 최고 점수 건물 선택
        best_building_id = _find_best_building(q["expected"])
        rank = np.where(ranked_ids == best_building_id)[0][0] + 1  # 1-indexed
        reciprocal_ranks.append(1.0 / rank)

    return round(sum(reciprocal_ranks) / len(reciprocal_ranks), 4)


def _find_best_building(expected: dict) -> int:
    """expected 조건에 가장 맞는 SAMPLE_BUILDINGS 인덱스 반환"""
    # 단순 점수화: 조건 맞으면 +1
    scores = []
    for b in SAMPLE_BUILDINGS:
        score = 0
        desc = b["desc"]
        for field, want_high in expected.items():
            # desc에서 숫자 추출하는 대신 label 기반으로 휴리스틱 판단
            if want_high and field in ("CCTV", "가로등", "편의점", "카페", "버스정류장", "병원", "식당"):
                if "번화가" in b["label"] or "안전" in b["label"] or field.replace("_", "") in b["label"]:
                    score += 1
            elif not want_high and field == "학교_거리":
                if "학교" in b["label"]:
                    score += 1
        scores.append(score)
    return int(np.argmax(scores))


# ─────────────────────────────────────────────
# 메인 실행
# ─────────────────────────────────────────────
def run():
    all_texts = [b["desc"] for b in SAMPLE_BUILDINGS]
    results = []

    for m in MODELS:
        print(f"\n[{m['name']}] 로딩 중...")

        # 메모리
        mem_mb = measure_memory(m["model_id"])

        # 모델 재로딩 (속도/품질 측정용)
        model = SentenceTransformer(m["model_id"])

        # 속도
        speed_ms = measure_speed(model, all_texts)

        # 검색 품질 (MRR)
        mrr = measure_retrieval_quality(model)

        results.append({
            "모델": m["name"],
            "설명": m["desc"],
            "속도(ms/건)": speed_ms,
            "메모리(MB)": mem_mb,
            "MRR": mrr,
        })
        print(f"  ✓ 속도: {speed_ms}ms/건 | 메모리: {mem_mb}MB | MRR: {mrr}")

    # 결과 출력
    print("\n\n========== 벤치마크 결과 ==========")
    print(tabulate(results, headers="keys", tablefmt="github"))

    # JSON 저장
    out_path = RESULTS_DIR / "benchmark_result.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    print(f"\n결과 저장: {out_path}")


if __name__ == "__main__":
    run()
