# Embedding Server (FastAPI) API 명세서

**서버**: `http://localhost:8000`  
**설명**: 자취인 프로젝트의 임베딩 및 벡터 검색 서비스 (Qdrant 기반)

> 이 서버는 Main Spring Server가 필요시 호출하는 보조 서비스입니다.

---

## 1. 헬스 체크

| 항목 | 값 |
|---|---|
| **요청 방식** | `GET` |
| **엔드포인트** | `/health` |
| **설명** | 서버 상태 확인 |
| **인증** | 불필요 |

### Response (200 OK)
```json
{
  "status": "ok"
}
```

---

## 2. 텍스트 임베딩

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/embed` |
| **설명** | 자연어 텍스트를 고차원 벡터로 변환 (Main Server에서 호출) |
| **인증** | 불필요 |

### Request
```json
{
  "text": "신촌역 근처 CCTV가 많은 투룸"
}
```

### Response (200 OK)
```json
{
  "vector": [0.12, -0.34, 0.56, ..., 0.10]
}
```

**주의**: 벡터의 차원은 임베딩 모델에 따라 결정됩니다 (예: 1536차원).

---

## 3. 벡터 기반 건물 검색

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/search` |
| **설명** | 사용자의 자연어 조건으로 가장 유사한 건물 검색 |
| **인증** | 불필요 |

### Request
```json
{
  "query": "CCTV 많고 카페 가까운 곳, 렌트료 400만원대",
  "top_k": 10
}
```

### Query Parameters / Body Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `query` | string | O | - | 사용자의 자연어 검색 조건 |
| `top_k` | integer | X | 20 | 반환할 상위 건물 개수 |

### Response (200 OK)
```json
{
  "results": [
    {
      "id": 1,
      "name": "남숭 하우스",
      "address": "서울시 종로구 ...",
      "latitude": 37.42,
      "longitude": 127.05,
      "price": 450000,
      "floor": 3,
      "features": ["CCTV", "카페 근처", "편의점"],
      "score": 0.87
    },
    {
      "id": 2,
      "name": "신촌 투룸",
      "address": "서울시 서대문구 ...",
      "latitude": 37.45,
      "longitude": 127.08,
      "price": 480000,
      "floor": 2,
      "features": ["엘리베이터", "카페 인근"],
      "score": 0.82
    }
  ]
}
```

**score**: 유사도 점수 (0~1 범위, 높을수록 사용자 조건과 유사)

---

## 사용 흐름

### 시나리오 1: Main Server에서 텍스트 임베딩
```
1. Main Server가 "/embed" 호출
2. FastAPI가 OpenAI/로컬 모델로 임베딩 생성
3. 벡터를 Main Server에 반환
```

### 시나리오 2: Main Server에서 건물 검색
```
1. Main Server가 "/search" 호출 (사용자 자연어 조건)
2. FastAPI가 임베딩 생성 → Qdrant 벡터 DB에서 유사도 검색
3. 상위 K개 건물을 Main Server에 반환
```

### 시나리오 3: Flutter 앱에서 직접 검색 (예정)
```
1. Flutter 앱이 "/search" 호출
2. FastAPI가 직접 결과 반환
3. Flutter가 지도에 건물 표시
```

---

## 설정 및 환경 변수

**.env 파일 예시**:
```env
QDRANT_HOST=localhost
QDRANT_PORT=6333
EMBEDDING_MODEL=sentence-transformers/all-MiniLM-L6-v2
OPENAI_API_KEY=sk-... (선택사항, OpenAI 사용 시)
```

---

## 미구현 기능 (TODO)

- [ ] 배치 임베딩 (`POST /embed-batch`) - 여러 텍스트 동시 임베딩
- [ ] 건물 색인화 (`POST /index-buildings`) - 기존 건물 데이터 임베딩
- [ ] 임베딩 모델 교체 (`POST /model/switch`) - 런타임 중 모델 변경
- [ ] 검색 필터링 (`POST /search/filtered`) - 가격, 지역 필터 추가
- [ ] 캐시 통계 (`GET /stats`) - 캐시 히트율, 쿼리 통계
