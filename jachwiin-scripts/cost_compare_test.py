"""
자취인 - 스토리지 10GB 추가 vs Claude API 토큰 증가 비용 비교

[Option A] EC2 20GB → 30GB 업그레이드 (FastAPI+Qdrant 유지)
  - 10GB 추가 비용: $0.08/GB/월 × 10 = $0.80/월 고정
  - Claude에는 Qdrant가 필터링한 top-K 건물만 전달 → 토큰 적음

[Option B] FastAPI 제거, Claude가 전체 건물 직접 처리
  - 스토리지 추가 비용 없음
  - Claude 입력 토큰 증가 (전체 건물 전달)

실험: 실제 Claude API를 호출해 쿼리당 토큰 사용량 측정 후 손익분기점 계산

실행: ANTHROPIC_API_KEY=... python3 cost_compare_test.py
"""

import anthropic
import json
import time

# ─────────────────────────────────────────────
# 가격 상수
# ─────────────────────────────────────────────
PRICE_INPUT_PER_M  = 3.0    # USD / 1M input tokens  (claude-sonnet-4-6)
PRICE_OUTPUT_PER_M = 15.0   # USD / 1M output tokens
KRW_PER_USD        = 1380
EBS_COST_PER_GB    = 0.08   # USD/GB/월 (AWS gp3)
EXTRA_GB           = 10     # 20→30GB 업그레이드 시 추가 용량

# ─────────────────────────────────────────────
# 건물 데이터
# Option A: Qdrant가 먼저 필터링 → Claude에 top-5만 전달
# Option B: FastAPI 없이 Claude에 전체 건물 전달
# ─────────────────────────────────────────────
ALL_BUILDINGS = [
    {"id": 1,  "지역": "관악구 신림동",   "편의점": 5, "카페": 3, "버스정류장": 4, "가로등": 20, "CCTV": 15, "병원": 2, "식당": 10, "학교_거리_m": 350},
    {"id": 2,  "지역": "관악구 봉천동",   "편의점": 3, "카페": 1, "버스정류장": 2, "가로등": 10, "CCTV": 8,  "병원": 1, "식당": 5,  "학교_거리_m": 700},
    {"id": 3,  "지역": "동작구 상도동",   "편의점": 4, "카페": 2, "버스정류장": 3, "가로등": 15, "CCTV": 12, "병원": 2, "식당": 8,  "학교_거리_m": 200},
    {"id": 4,  "지역": "마포구 연남동",   "편의점": 6, "카페": 8, "버스정류장": 5, "가로등": 25, "CCTV": 20, "병원": 3, "식당": 15, "학교_거리_m": 450},
    {"id": 5,  "지역": "서대문구 홍제동", "편의점": 2, "카페": 1, "버스정류장": 3, "가로등": 8,  "CCTV": 5,  "병원": 1, "식당": 4,  "학교_거리_m": 900},
    {"id": 6,  "지역": "성북구 길음동",   "편의점": 4, "카페": 2, "버스정류장": 4, "가로등": 18, "CCTV": 10, "병원": 2, "식당": 7,  "학교_거리_m": 300},
    {"id": 7,  "지역": "노원구 중계동",   "편의점": 3, "카페": 2, "버스정류장": 3, "가로등": 12, "CCTV": 9,  "병원": 2, "식당": 6,  "학교_거리_m": 150},
    {"id": 8,  "지역": "강남구 역삼동",   "편의점": 8, "카페": 12,"버스정류장": 6, "가로등": 30, "CCTV": 25, "병원": 5, "식당": 20, "학교_거리_m": 600},
    {"id": 9,  "지역": "송파구 잠실동",   "편의점": 7, "카페": 6, "버스정류장": 5, "가로등": 22, "CCTV": 18, "병원": 4, "식당": 12, "학교_거리_m": 800},
    {"id": 10, "지역": "은평구 불광동",   "편의점": 3, "카페": 2, "버스정류장": 4, "가로등": 14, "CCTV": 7,  "병원": 1, "식당": 5,  "학교_거리_m": 400},
    {"id": 11, "지역": "종로구 혜화동",   "편의점": 4, "카페": 5, "버스정류장": 4, "가로등": 16, "CCTV": 11, "병원": 3, "식당": 9,  "학교_거리_m": 100},
    {"id": 12, "지역": "중랑구 면목동",   "편의점": 2, "카페": 1, "버스정류장": 3, "가로등": 9,  "CCTV": 6,  "병원": 1, "식당": 4,  "학교_거리_m": 550},
    {"id": 13, "지역": "광진구 화양동",   "편의점": 5, "카페": 4, "버스정류장": 4, "가로등": 17, "CCTV": 13, "병원": 2, "식당": 8,  "학교_거리_m": 80},
    {"id": 14, "지역": "용산구 한강로",   "편의점": 6, "카페": 5, "버스정류장": 5, "가로등": 24, "CCTV": 19, "병원": 4, "식당": 14, "학교_거리_m": 500},
    {"id": 15, "지역": "강서구 화곡동",   "편의점": 3, "카페": 2, "버스정류장": 4, "가로등": 13, "CCTV": 8,  "병원": 2, "식당": 6,  "학교_거리_m": 650},
]

# Option A: Qdrant 필터링 후 top-5만 Claude에 전달 (시뮬레이션)
TOP_K_BUILDINGS = ALL_BUILDINGS[:5]

SYSTEM_PROMPT = """자취방 추천 AI입니다. 건물 목록에서 조건에 맞는 상위 3곳을 추천하세요.
JSON으로만 응답: {"recommendations": [{"building_id": int, "reason": "한 문장"}]}"""

TEST_QUERIES = [
    "CCTV 많고 편의점 가까운 안전한 곳",
    "카페 많고 학교 가까운 자취방",
    "버스정류장 많고 가로등 잘 된 동네",
]


def cost_usd(input_tok: int, output_tok: int) -> float:
    return (input_tok / 1e6) * PRICE_INPUT_PER_M + (output_tok / 1e6) * PRICE_OUTPUT_PER_M


def run_option(client, label: str, buildings: list, queries: list) -> dict:
    """한 옵션에 대해 모든 쿼리 실행 후 평균 토큰/비용 반환"""
    buildings_json = json.dumps(buildings, ensure_ascii=False)
    total_in, total_out, total_time = 0, 0, 0.0

    print(f"\n{'─'*60}")
    print(f"  {label}  (건물 수: {len(buildings)}개)")
    print(f"{'─'*60}")

    for i, query in enumerate(queries, 1):
        user_msg = f"조건: {query}\n\n건물 목록:\n{buildings_json}"
        t0 = time.time()
        resp = client.messages.create(
            model="claude-sonnet-4-6",
            max_tokens=300,
            system=SYSTEM_PROMPT,
            messages=[{"role": "user", "content": user_msg}],
        )
        elapsed = time.time() - t0
        in_tok  = resp.usage.input_tokens
        out_tok = resp.usage.output_tokens
        cost    = cost_usd(in_tok, out_tok)

        total_in   += in_tok
        total_out  += out_tok
        total_time += elapsed

        print(f"  Q{i}: \"{query}\"")
        print(f"       입력 {in_tok:,}tok / 출력 {out_tok:,}tok / {elapsed:.2f}초 / ${cost:.6f} (₩{cost*KRW_PER_USD:.2f})")

    n = len(queries)
    return {
        "label":        label,
        "buildings":    len(buildings),
        "avg_in_tok":   total_in  // n,
        "avg_out_tok":  total_out // n,
        "avg_time":     total_time / n,
        "avg_cost_usd": cost_usd(total_in // n, total_out // n),
    }


def main():
    client = anthropic.Anthropic()

    print("=" * 60)
    print("  자취인 - 스토리지 10GB 추가 vs Claude 토큰 증가 비교")
    print("  모델: claude-sonnet-4-6")
    print("=" * 60)

    r_a = run_option(client, "[Option A] FastAPI 유지 - Qdrant top-5만 Claude에 전달", TOP_K_BUILDINGS, TEST_QUERIES)
    r_b = run_option(client, "[Option B] FastAPI 제거 - 전체 건물 Claude에 전달",     ALL_BUILDINGS,   TEST_QUERIES)

    # ─────────────────────────────────────────────
    # 비용 분석
    # ─────────────────────────────────────────────
    storage_upgrade_cost = EBS_COST_PER_GB * EXTRA_GB   # $0.80/월
    token_diff_per_query = r_b["avg_cost_usd"] - r_a["avg_cost_usd"]
    # Option B가 비싸질 때까지 허용 가능한 쿼리 수
    # storage_upgrade = token_diff × queries  →  queries = storage_upgrade / token_diff
    breakeven_queries = int(storage_upgrade_cost / token_diff_per_query) if token_diff_per_query > 0 else float("inf")

    print(f"\n{'='*60}")
    print("  비용 분석 요약")
    print(f"{'='*60}")
    print(f"\n  {'항목':<30} {'Option A (스토리지+10GB)':<25} {'Option B (Claude 직접)'}")
    print(f"  {'─'*80}")
    print(f"  {'평균 입력 토큰':<30} {r_a['avg_in_tok']:>10,} tok           {r_b['avg_in_tok']:>10,} tok")
    print(f"  {'평균 출력 토큰':<30} {r_a['avg_out_tok']:>10,} tok           {r_b['avg_out_tok']:>10,} tok")
    print(f"  {'쿼리당 Claude 비용':<30} ${r_a['avg_cost_usd']:.6f}              ${r_b['avg_cost_usd']:.6f}")
    print(f"  {'쿼리당 Claude 비용 (₩)':<30} ₩{r_a['avg_cost_usd']*KRW_PER_USD:.4f}             ₩{r_b['avg_cost_usd']*KRW_PER_USD:.4f}")
    print(f"  {'스토리지 고정비':<30} +${storage_upgrade_cost:.2f}/월 고정              없음")
    print(f"  {'평균 응답시간':<30} {r_a['avg_time']:.2f}초                    {r_b['avg_time']:.2f}초")

    print(f"\n  [쿼리당 토큰 증가 비용]  ${token_diff_per_query:.6f}  (₩{token_diff_per_query*KRW_PER_USD:.4f})")
    print(f"  [스토리지 10GB 추가비]   ${storage_upgrade_cost:.2f}/월")

    print(f"\n  [손익분기점]")
    print(f"  월 {breakeven_queries:,}건 기준:")
    print(f"    - 월 쿼리 < {breakeven_queries:,}건  →  Option B (Claude 직접) 유리")
    print(f"    - 월 쿼리 ≥ {breakeven_queries:,}건  →  Option A (스토리지 추가) 유리")

    print(f"\n  [시나리오별 월 비용]")
    scenarios = [50, 100, 500, 1000, 2000, breakeven_queries + 100]
    print(f"  {'월 쿼리':<12} {'Option A 추가 비용':<25} {'Option B 추가 비용':<25} {'유리한 옵션'}")
    print(f"  {'─'*70}")
    for q in scenarios:
        cost_a = storage_upgrade_cost + r_a["avg_cost_usd"] * q   # 스토리지 고정 + Claude
        cost_b = r_b["avg_cost_usd"] * q                           # Claude만 (스토리지 추가 없음)
        winner = "A (스토리지+10GB)" if cost_a < cost_b else "B (Claude 직접)"
        print(f"  {q:<12,} ${cost_a:<24.4f} ${cost_b:<24.4f} {winner}")

    print(f"\n  [결론]")
    print(f"  ✔ 캡스톤 데모 수준(월 ~{min(100, breakeven_queries//2)}건)은 Option B가 더 저렴")
    print(f"  ✔ 스토리지 문제 완전 해소 + FastAPI/Qdrant 인프라 제거로 운영 단순화")
    print(f"  ✔ 월 {breakeven_queries:,}건 초과 시점에 Option A 재검토 필요")


if __name__ == "__main__":
    main()
