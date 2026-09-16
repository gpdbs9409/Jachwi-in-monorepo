# Main Server API 명세서

**서버**: `http://localhost:8080`  
**설명**: 자취인 프로젝트의 main 비즈니스 로직 API (지도, 게시판, LLM 기반 추천)

---

## 인증 (Authentication)

대부분의 API는 JWT 토큰을 사용합니다.  
**Header**: `Authorization: Bearer {accessToken}`

---

## 1. 지도 API

### 1-1. 지도 범위 내 건물 조회

| 항목 | 값 |
|---|---|
| **요청 방식** | `GET` |
| **엔드포인트** | `/api/v1/map/view/position` |
| **설명** | 지도 범위(사각형 영역)에 포함된 건물 목록 조회 |
| **인증** | 불필요 |

### Request
```
GET /api/v1/map/view/position?minX=37.4&maxX=37.5&minY=127.0&maxY=127.1
```

### Query Parameters
| 파라미터 | 타입 | 설명 |
|---|---|---|
| `minX` | number | 최소 위도 |
| `maxX` | number | 최대 위도 |
| `minY` | number | 최소 경도 |
| `maxY` | number | 최대 경도 |

### Response (200 OK)
```json
[
  {
    "id": 1,
    "name": "남숭 하우스",
    "address": "서울시 종로구 ...",
    "latitude": 37.42,
    "longitude": 127.05,
    "price": 450000,
    "floor": 3,
    "features": ["CCTV", "카페 근처", "편의점"]
  },
  {
    "id": 2,
    "name": "신촌 투룸",
    "address": "서울시 서대문구 ...",
    "latitude": 37.45,
    "longitude": 127.08,
    "price": 380000,
    "floor": 2,
    "features": ["엘리베이터", "세탁실"]
  }
]
```

---

### 1-2. 지도 범위 전송 (위치 업데이트)

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/api/v1/map/bounds` |
| **설명** | 클라이언트가 현재 보고 있는 지도 범위를 서버에 전송 (통계/추천 용도) |
| **인증** | 필요 (로그인 필수) |

### Request
```json
{
  "minX": 37.4,
  "maxX": 37.5,
  "minY": 127.0,
  "maxY": 127.1
}
```

### Response (200 OK)
```json
"범위 정보가 저장되었습니다."
```

---

### 1-3. 마커 수 조회

| 항목 | 값 |
|---|---|
| **요청 방식** | `GET` |
| **엔드포인트** | `/api/v1/map/markers` |
| **설명** | 전체 건물/마커의 총 개수 조회 |
| **인증** | 불필요 |

### Response (200 OK)
```json
1250
```

---

## 2. 게시판 API (TODO)

### 2-1. 게시글 목록 조회 (예정)

| 항목 | 값 |
|---|---|
| **요청 방식** | `GET` |
| **엔드포인트** | `/api/v1/posts` |
| **설명** | 카테고리별 게시글 목록 조회 (페이징 지원) |

### Query Parameters
| 파라미터 | 타입 | 설명 |
|---|---|---|
| `category` | string | 카테고리 (공동구매, 룸메이트, 방구하기, 일반) |
| `page` | number | 페이지 번호 (기본값: 0) |
| `size` | number | 한 페이지당 항목 수 (기본값: 20) |
| `sort` | string | 정렬 (latest, popular) |

### Response (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "title": "용산역 근처 룸메이트 구합니다",
      "category": "룸메이트",
      "author": "사용자명",
      "viewCount": 45,
      "likeCount": 12,
      "commentCount": 3,
      "createdAt": "2026-04-03T15:30:00"
    }
  ],
  "totalPages": 10,
  "totalElements": 200,
  "currentPage": 0,
  "hasNext": true
}
```

---

### 2-2. 게시글 상세 조회 (예정)

| 항목 | 값 |
|---|---|
| **요청 방식** | `GET` |
| **엔드포인트** | `/api/v1/posts/{postId}` |
| **설명** | 특정 게시글의 상세 내용 조회 |

### Response (200 OK)
```json
{
  "id": 1,
  "title": "용산역 근처 룸메이트 구합니다",
  "content": "안녕하세요, ...",
  "category": "룸메이트",
  "author": "사용자명",
  "viewCount": 45,
  "likeCount": 12,
  "createdAt": "2026-04-03T15:30:00",
  "updatedAt": "2026-04-03T15:35:00",
  "comments": [
    {
      "id": 1,
      "author": "댓글작성자",
      "content": "저도 관심있습니다.",
      "createdAt": "2026-04-03T15:45:00"
    }
  ]
}
```

---

### 2-3. 게시글 작성 (예정)

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/api/v1/posts` |
| **설명** | 새로운 게시글 작성 |
| **인증** | 필요 (로그인 필수) |

### Request
```json
{
  "title": "용산역 근처 룸메이트 구합니다",
  "content": "안녕하세요, ...",
  "category": "룸메이트"
}
```

### Response (201 Created)
```json
{
  "id": 1,
  "message": "게시글이 등록되었습니다."
}
```

---

## 3. LLM API

### 3-1. 게시글 자동 분류

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/api/v1/llm/classify` |
| **설명** | 게시글 제목/내용으로부터 카테고리 자동 분류 |
| **인증** | 불필요 |

### Request
```json
{
  "title": "신촌역 근처에서 함께 살 룸메이트를 찾습니다",
  "content": "안녕하세요. 저는 서울대 대학원생이고 ..."
}
```

### Response (200 OK)
```json
"룸메이트"
```

---

### 3-2. 자취방 추천 (LLM 기반)

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/api/v1/llm/recommend` |
| **설명** | Claude AI를 통한 사용자 조건 기반 자취방 추천 |
| **인증** | 필요 (로그인 필수) |

### Request
```json
{
  "budget": 450000,
  "location": "용산역 근처",
  "preferences": "CCTV 많고 카페 가까운 곳"
}
```

### Response (200 OK)
```json
"추천 이유: ...\n건물 1: ...\n건물 2: ..."
```

---

## 4. 위치 / 실시간 데이터 API

### 4-1. 위치 전송 (위치 기반 서비스용)

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/api/v1/location/send` |
| **설명** | 클라이언트의 현재 위치를 Kafka 토픽으로 발행 (위치 기반 추천/분석 용도) |
| **인증** | 필요 (로그인 필수) |

### Request
```json
{
  "userId": 1,
  "latitude": 37.42,
  "longitude": 127.05,
  "speed": 0,
  "accuracy": 10.5,
  "timestamp": "2026-04-04T10:30:00Z"
}
```

### Response (200 OK)
```json
"위치 이벤트 발행 완료"
```

---

## 미구현 기능 (TODO)

### 댓글 API
- [ ] 댓글 작성 (`POST /api/v1/posts/{postId}/comments`)
- [ ] 댓글 목록 조회 (`GET /api/v1/posts/{postId}/comments`)
- [ ] 댓글 수정 (`PATCH /api/v1/comments/{commentId}`)
- [ ] 댓글 삭제 (`DELETE /api/v1/comments/{commentId}`)

### 사용자 API
- [ ] 사용자 정보 조회 (`GET /api/v1/users/me`)
- [ ] 프로필 수정 (`PATCH /api/v1/users/me`)
- [ ] 사용자 활동 조회 (`GET /api/v1/users/me/activity`)

### 상호작용 API
- [ ] 게시글 좋아요 (`POST /api/v1/posts/{postId}/like`)
- [ ] 게시글 좋아요 취소 (`DELETE /api/v1/posts/{postId}/like`)
- [ ] 게시글 북마크 (`POST /api/v1/posts/{postId}/bookmark`)
- [ ] 북마크 목록 조회 (`GET /api/v1/users/me/bookmarks`)

### 검색 API
- [ ] 게시글 검색 (`GET /api/v1/posts/search`)
- [ ] 건물 검색 (임베딩 기반, FastAPI 연동)

### 메시지 API
- [ ] 메시지 조회 (`GET /api/v1/messages`)
- [ ] 메시지 전송 (`POST /api/v1/messages`)
