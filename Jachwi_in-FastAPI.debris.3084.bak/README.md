# Embedding Server (FastAPI) - README

**자취인 프로젝트 임베딩 & 벡터 검색 서비스**

---

## 개요

Embedding Server는 자취인 프로젝트의 **LLM 임베딩 및 벡터 검색** 기능을 담당하는 보조 서비스입니다.

- **포트**: 8000
- **프레임워크**: FastAPI
- **언어**: Python 3.9+
- **벡터 DB**: Qdrant
- **주요 기능**:
  - 자연어 텍스트 → 벡터 변환
  - 벡터 기반 유사도 검색
  - Main Server와의 REST 연동

> Main Server에서 LLM 기반 추천 시 FastAPI를 호출합니다.

---

## 프로젝트 구조

```
Jachwi_in-FastAPI/
├── app/
│   ├── __init__.py
│   ├── main.py                # FastAPI 앱, 라우트 정의
│   ├── embedder.py            # 임베딩 모델 (sentence-transformers)
│   ├── vector_store.py        # Qdrant 클라이언트
│   └── models.py              # Pydantic 모델 (예정)
├── scripts/
│   └── ingest.py              # 초기 건물 데이터 임베딩
├── data/
│   └── queries.py             # 테스트용 쿼리 예시
├── results/                   # 검색 결과 캐시
├── requirements.txt           # Python 의존성
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── API.md                     # API 명세서
```

---

## 로컬 환경 설정

### 1. 필수 설치 항목
- Python 3.9 이상
- Qdrant (벡터 DB)
- pip 또는 poetry

### 2. Python 가상환경 구성

```bash
# 프로젝트 디렉토리 진입
cd Jachwi_in-FastAPI

# 가상환경 생성
python3 -m venv venv

# 가상환경 활성화
source venv/bin/activate  # macOS/Linux
# 또는
venv\Scripts\activate  # Windows
```

### 3. 의존성 설치

```bash
pip install -r requirements.txt
```

### 4. Qdrant 실행

#### 방법 1: Docker (권장)

```bash
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -v $(pwd)/qdrant_storage:/qdrant/storage:z \
  qdrant/qdrant:latest
```

#### 방법 2: Docker Compose

```bash
docker-compose up -d qdrant
```

**Qdrant 확인**:
```bash
curl http://localhost:6333/health
# {"status":"ok"}
```

### 5. 환경 변수 설정

**.env 파일 생성**:
```env
QDRANT_HOST=localhost
QDRANT_PORT=6333
EMBEDDING_MODEL=sentence-transformers/all-MiniLM-L6-v2
COLLECTION_NAME=buildings
LOG_LEVEL=INFO
```

### 6. 서버 실행

```bash
# 기본 실행
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 또는
python -m uvicorn app.main:app --reload --port 8000
```

**실행 확인**:
```bash
curl http://localhost:8000/health
# {"status":"ok"}

# API 문서
open http://localhost:8000/docs  # Swagger UI
```

---

## API 사용 예시

자세한 API 명세서는 [API.md](./API.md)를 참조하세요.

### 1. 텍스트 임베딩

```bash
curl -X POST http://localhost:8000/embed \
  -H "Content-Type: application/json" \
  -d '{"text": "신촌역 근처 CCTV가 많은 투룸"}'
```

### 2. 벡터 기반 검색

```bash
curl -X POST http://localhost:8000/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "CCTV 많고 카페 가까운 곳, 렌트료 400만원대",
    "top_k": 10
  }'
```

---

## Main Server와의 연동

Main Server (port 8080)에서 FastAPI를 호출하는 예시:

```java
// LlmService.java
@Service
public class LlmService {
    
    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiUrl;
    
    public List<Building> searchSimilarBuildings(String userCondition) {
        RestTemplate restTemplate = new RestTemplate();
        String url = fastApiUrl + "/search";
        
        Map<String, Object> request = new HashMap<>();
        request.put("query", userCondition);
        request.put("top_k", 10);
        
        // POST 요청
        SearchResponse response = restTemplate.postForObject(url, request, SearchResponse.class);
        
        return response.getResults();
    }
}
```

---

## Docker를 통한 실행

```bash
# FastAPI + Qdrant 함께 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 종료
docker-compose down
```

---

## 추가 개발 예정 기능

- [ ] 배치 임베딩 API (`POST /embed-batch`)
- [ ] 건물 색인화 API (`POST /index-buildings`)
- [ ] 임베딩 모델 교체 (`POST /model/switch`)
- [ ] 검색 필터링 (`POST /search/filtered`) - 가격, 지역 필터
- [ ] 캐시 통계 (`GET /stats`)

---

**마지막 업데이트**: 2026-04-04
