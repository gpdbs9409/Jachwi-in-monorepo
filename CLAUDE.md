# CLAUDE.md — 자취인(Jachwi-in) 전체 프로젝트 컨텍스트

## 프로젝트 개요
자취생을 위한 AI 기반 지도 + 커뮤니티 플랫폼 (캡스톤 프로젝트)

---

## 서비스 구성

| 서비스 | 레포 | 포트 | 역할 |
|--------|------|------|------|
| Auth Server | `Jachwi_in-Auth-Server` | 8081 | 회원가입, 로그인, JWT 발급 |
| Main Server | `Jachwi_in-Server-Spring` | 8080 | 지도, 커뮤니티, LLM 추천 |
| Embedding Server | `Jachwi_in-FastAPI` | 8000 | 벡터 임베딩, Qdrant 유사 검색 |
| Web App | `Jachwi_in-Web-React` | 5173(dev) | React(Vite) 웹 클라이언트 |
| Benchmark | `Jachwi_in-Embedding-Benchmark` | - | 성능 비교용 사이드 프로젝트 (건드리지 말 것) |

> 2026-09 기준: 프론트엔드는 Flutter(`Jachwi_in-App-Flutter-main`)에서 React(`Jachwi_in-Web-React`)로 전환.
> 기존 Flutter 레포는 삭제하지 않고 `_archived_Jachwi_in-App-Flutter-main` 으로 보관만 해둠 (필요 없으면 지워도 됨).

---

## 서버간 통신 흐름

```
React (Vite, :5173)
  ├── POST /auth/login, GET /auth/join/mailConfirm/{email}, POST /auth/join
  │         → Auth Server (8081)
  │               └── GET /auth/internal/users/{email}  ← Main Server가 호출
  └── GET /api/v1/map/view/position, /api/v1/posts/**, /api/v1/llm/**
            → Main Server (8080)
                  └── POST /search → FastAPI (8000) → Qdrant (6333)
                      (FastAPI 장애/미색인 시 DB 좌표범위 검색으로 자동 폴백)

/map 페이지 AI 채팅 추천 흐름:
  NaverMap 뷰포트(minX/maxX/minY/maxY) + 채팅 입력
    → POST /api/v1/llm/chat (Main Server)
        1. Haiku로 자연어 → {budget, preferences, mood} 파싱
        2. FastAPI /search 호출 시 뷰포트를 Qdrant range filter로 강제
           (위치=필터로 강제, 벡터유사도=선호/분위기만 좁히는 용도)
        3. Qdrant 결과 없으면 DB 뷰포트 범위 + 휴리스틱 점수로 폴백
        4. Sonnet이 후보 중 2~4곳을 골라 이유와 함께 답변 생성
    → 응답의 buildings[]를 지도 위에 강조 마커로 표시 + fitBounds
```

프론트가 백엔드와 다른 origin(포트)에서 뜨기 때문에 Auth Server / Main Server 양쪽에
`CorsConfig`(`config/CorsConfig.java`)를 추가해 CORS를 허용해둠. 허용 origin은
`CORS_ALLOWED_ORIGINS` 환경변수(콤마 구분, 기본값 `http://localhost:5173,http://localhost:4173`)로
배포 시(Vercel 도메인) 덮어쓰면 됨.

---

## DB 구조 (MySQL `jachwiin_db`)

```sql
users       -- Auth Server owns (write), Main Server reads via API
building    -- 건물/시설 데이터. id + UNIQUE(x,y) + generated location POINT SRID 4326 + SPATIAL INDEX
room_listing -- 실제 매물; trade_type=SALE/JEONSE/MONTHLY_RENT
building_trade -- 통합 실거래 이력 (기존 apt_trade 원천 테이블 유지)
-- cafe/convenience_store/hospital/restaurant/cctv/streetlight/school/subway_station/bus_stop: POI
posts       -- 커뮤니티 게시글. category ENUM: QUESTION|REVIEW|TIP|INFO|ROOMMATE|ETC
comments    -- 댓글 + 대댓글 (parent_id 자기참조)
bookmarks   -- 관심 건물 (user_id + building_id 복합 유니크)
```

---

## 인프라 (로컬/운영 공통)

| 서비스 | 용도 |
|--------|------|
| MySQL :3306 | 메인 DB |
| Redis :6379 | LLM 결과 캐시(1h), 이메일 인증코드(5m TTL) |
| Qdrant :6333 | 건물 벡터 DB (FastAPI 전용) |

- docker-compose: `Jachwi_in-Server-Spring/docker-compose.yml`
- Qdrant 초기 적재 (docker-compose 기동 후, MySQL에 building 데이터가 있어야 함):
  `docker-compose exec embedding-service python -m scripts.ingest`
  point id로 MySQL `building.id`를 그대로 사용하므로 재실행해도 안전하게 덮어씀.
  building 데이터가 갱신되면 다시 실행해줘야 Qdrant도 최신 상태가 됨 (자동 동기화 없음).

---

## 환경변수 (운영 시 필수)

| 변수 | 서버 | 설명 |
|------|------|------|
| `JWT_SECRET` | Auth + Main | 동일한 값이어야 함 (최소 32자) |
| `CLAUDE_API_KEY` | Main | Anthropic API 키 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Auth | 환경변수로 설정 |

---

## 주요 API 엔드포인트

### Auth Server (8081)
```
POST /auth/login                     로그인 → {accessToken, refreshToken}
GET  /auth/join/mailConfirm/{email}  이메일 인증코드 발송
POST /auth/join/mailConfirm          인증코드 검증 {email, ePw}
POST /auth/join                      회원가입 {email, name, nickname, password, school}
GET  /auth/internal/users/{email}    내부용: Main Server가 사용자 정보 조회
```

### Main Server (8080)
```
GET  /api/v1/map/view/position        건물 조회 (query: minX, maxX, minY, maxY)
GET  /api/v1/posts                    게시글 목록 (query: category, page, size)
GET  /api/v1/posts/{id}               게시글 상세 (조회수 +1)
POST /api/v1/posts                    게시글 작성 (JWT 필요)
PUT  /api/v1/posts/{id}               게시글 수정 (본인만)
DEL  /api/v1/posts/{id}               게시글 삭제 (본인만)
GET  /api/v1/posts/{id}/comments      댓글 목록
POST /api/v1/posts/{id}/comments      댓글 작성 (JWT, {content, parentId?})
DEL  /api/v1/posts/{id}/comments/{cid} 댓글 삭제 (본인만)
POST /api/v1/llm/classify             게시글 분류 (Claude)
POST /api/v1/llm/recommend            자취방 추천 (수동 폼 - 레거시, 하위호환용)
POST /api/v1/llm/chat                 /map 페이지 자연어 채팅 추천 (신규, 지도 뷰포트 기반)
```

### FastAPI (8000)
```
GET  /health
POST /embed   {text} → {vector}
POST /search  {query, top_k, min_x?, max_x?, min_y?, max_y?} → {results: [{score, building}]}
             (min/max_x/y 주면 Qdrant range filter로 해당 좌표범위 안의 건물만 검색)
```

---

## Building 엔티티 주의사항

- Java 필드명은 영문 (province, district, cafe, cctv 등)
- DB 컬럼명은 기존 한글 유지 (@Column(name="시도명") 방식)
- `BuildingCpk` 삭제됨. PK는 `id(BIGINT)`, 좌표는 UNIQUE INDEX(x,y)

---

## 브랜치 전략

React 웹 앱을 포함해 모든 레포가 `main` 단일 브랜치 사용 중.
(예전 Flutter 레포는 `master`/`develop`/`feat/*` 전략을 썼으나, 레포 자체가 보관 처리되어 더 이상 해당 없음.)

---

---

## /map 페이지 AI 채팅 추천 (2026-09 추가)

### 기획 배경
자소서에 썼던 "자연어로 조건 말하면 방 추천" 기능이 서비스의 핵심 기능으로 재정의됨.
기존에는 `/recommend`에 학교명·중심좌표·반경을 수동으로 입력하는 폼이 있었는데(레거시로 유지),
이번에 `/map` 페이지에 채팅 패널을 얹는 방식으로 흡수함. 별도 페이지로 안 만든 이유:

- 지도 자체가 이미 "지금 보고 있는 영역"이라는 검색 범위를 갖고 있음 → 폼에서 중심좌표/반경을
  다시 입력받는 것은 중복이고 사용성이 떨어짐. 지도를 움직이는 행위 자체가 검색 범위 지정이 되도록 함.
- 추천 결과를 "지도 위에 바로" 강조 마커로 찍어줘야 사용자가 "한눈에" 보고 판단할 수 있음
  → 지도와 분리된 페이지였다면 다시 지도로 돌아가서 위치를 대조해봐야 하는 번거로움이 생김.

UI: `.map-canvas` 우하단에 떠 있는 챗봇 패널(`MapChatPanel`). 접었다 펼 수 있는 FAB 버튼 형태.
대화 메모리는 백엔드에 저장하지 않고(무상태 서버 유지), 프론트 state가 최근 6턴을 들고 있다가
매 요청마다 함께 보내는 방식 — 서버 재시작/스케일아웃에 영향받지 않으면서도 "거기서 카페있는 데만"
같은 후속 질문을 자연스럽게 처리 가능.

### 백엔드 설계
`POST /api/v1/llm/chat` (`ChatRecommendRequestDto` → `ChatRecommendResponseDto`), `LlmService.chatRecommend()`:

1. **조건 파싱 (Haiku)**: 자연어 발화 → `{budget, preferences[], mood}` JSON. 저비용 모델로 구조화.
2. **위치는 필터로 강제, 벡터는 정성적 조건만 좁히는 데 사용**: FastAPI `/search`에 지도 뷰포트
   (minX/maxX/minY/maxY)를 함께 보내 Qdrant range filter로 후보를 그 영역 안으로 강제한 뒤,
   그 안에서 임베딩 유사도로 "선호시설/분위기" 조건을 좁힘. (아래 "왜 이게 필요한가" 참고)
3. **DB 폴백**: Qdrant가 비어있거나 FastAPI 장애 시, 같은 뷰포트로 DB 좌표범위 검색 + 언급된
   선호시설 보유 수를 합산하는 휴리스틱 점수로 정렬 (완전 무작위 순서보다는 나은 대체 로직).
4. **최종 답변 (Sonnet)**: 후보 2~4곳 + 대화 맥락을 주고 채팅체로 답변 생성. 임대료 데이터가
   없다는 점을 프롬프트에 명시해 숫자를 지어내지 않도록 방어.
5. 응답의 `buildings[]`(id/x/y/address/score)를 프론트가 지도 강조 마커로 렌더링 + `fitBounds`.

기존 `recommendRooms()`(수동 폼)도 같은 버그를 하나 갖고 있었음: `budget`을 DTO로 받아놓고
실제 Claude 프롬프트에는 전달하지 않아 예산이 조용히 무시되고 있었음 — 이번에 같이 고침.


### 실행 순서 (로컬)
```bash
docker-compose up -d --build          # 6개 컨테이너 기동
# building 테이블에 데이터가 있는 상태에서:
docker-compose exec embedding-service python -m scripts.ingest
```
ingest를 실행하지 않아도 서비스는 깨지지 않음 — DB 폴백으로 계속 동작하되 추천 품질이 낮음.

## 제거된 것들 (참고)

- **Kafka + Zookeeper**: 위치 이벤트 발행-소비 구조였으나 결과를 클라이언트에 전달할 방법 없어 제거
  - LocationController, LocationProducer, LocationConsumer, LocationDto, KafkaConfig 전부 삭제
  - 지도 기능은 MapController의 GET /api/v1/map/view/position으로 충분
