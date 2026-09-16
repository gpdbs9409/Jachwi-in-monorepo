# Main Server (자취인 - Spring Boot :8080)

자취인 프로젝트의 메인 비즈니스 로직 서버.

---

## 서버 실행 방법

### 1. 인프라 먼저 띄우기 (MySQL + Redis + Qdrant)

```bash
cd Jachwi_in-Server-Spring
docker-compose up -d mysql redis qdrant
```

> Auth Server, FastAPI 없이 Main Server만 테스트할 때는 위 3개만 켜도 됨.

### 2. 환경변수 설정

```bash
export JWT_SECRET=jachwi-in-secret-key-must-be-at-least-32-characters
export CLAUDE_API_KEY=sk-ant-...
```

> `application.properties`에 기본값이 있어서 로컬 테스트 시 `CLAUDE_API_KEY`만 필수.

### 3. 서버 실행

```bash
./gradlew bootRun
```

또는 JAR 빌드 후 실행:

```bash
./gradlew build -x test
java -jar build/libs/*.jar
```

### 4. 동작 확인

```bash
# 건물 조회 (apt_trade API)
curl "http://localhost:8080/api/v1/apt-trade?sigungu=강남구&page=0&size=5"

# 지도 범위 내 건물 조회
curl "http://localhost:8080/api/v1/map/view/position?minX=126.9&maxX=127.1&minY=37.4&maxY=37.6"
```

---

## 의존 서비스 현황

| 서비스 | 포트 | 필수 여부 | 용도 |
|--------|------|-----------|------|
| MySQL | 3306 | 필수 | 메인 DB |
| Redis | 6379 | 필수 | LLM 결과 캐시 (1h TTL) |
| Qdrant | 6333 | 선택 | 벡터 검색 (없으면 DB 폴백) |
| Auth Server | 8081 | 선택 | JWT 사용자 정보 조회 |
| FastAPI | 8000 | 선택 | 임베딩 벡터 검색 (없으면 DB 폴백) |

---

## 주요 API

### 지도
```
GET /api/v1/map/view/position?minX=&maxX=&minY=&maxY=
```

### 아파트 실거래가
```
GET /api/v1/apt-trade?sigungu=강남구&dong=역삼동&dealYm=202503&page=0&size=20
```
- `sigungu`, `dong`, `dealYm` 모두 선택 파라미터

### 게시판
```
GET    /api/v1/posts?category=QUESTION&page=0&size=10
GET    /api/v1/posts/{id}
POST   /api/v1/posts                   # JWT 필요
PUT    /api/v1/posts/{id}              # JWT 필요 (본인만)
DELETE /api/v1/posts/{id}             # JWT 필요 (본인만)
GET    /api/v1/posts/{id}/comments
POST   /api/v1/posts/{id}/comments    # JWT 필요
DELETE /api/v1/posts/{id}/comments/{cid}  # JWT 필요
```

### LLM
```
POST /api/v1/llm/classify     {title, content} → 카테고리 문자열
POST /api/v1/llm/recommend    {school, preferences, centerX, centerY, radius} → 추천 텍스트
```

---

## 환경변수 목록

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `JWT_SECRET` | (코드 내 기본값) | Auth Server와 동일해야 함, 최소 32자 |
| `CLAUDE_API_KEY` | 없음 (필수) | Anthropic API 키 |
| `SPRING_DATASOURCE_URL` | localhost:3306/jachwi | MySQL 연결 |
| `REDIS_HOST` | localhost | Redis 호스트 |
| `FASTAPI_BASE_URL` | http://localhost:8000 | FastAPI 주소 |
| `AUTH_SERVER_BASE_URL` | http://localhost:8081 | Auth Server 주소 |
