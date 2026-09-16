# CLAUDE.md — Main Server (Spring Boot :8080)

## 패키지 구조
```
com.capstone.Jachwi_inServerSpring
├── client/         외부 서버 HTTP 클라이언트
│   ├── AuthServerClient       → Auth Server GET /auth/internal/users/{email}
│   ├── FastApiClient          → FastAPI POST /search (벡터 검색)
│   ├── FastApiSearchRequest/Response
│   └── UserInfoDto            (Auth Server 응답 DTO)
├── config/
│   ├── ClaudeConfig           Anthropic SDK 빈 등록
│   ├── JwtFilter / JwtUtil    JWT 검증 (Auth Server와 동일 시크릿)
│   ├── RestTemplateConfig     connectTimeout 3s / readTimeout 10s
│   ├── SecurityConfig         GET /api/v1/posts/** 공개, 나머지 JWT 필요
│   └── webConfig              CORS (Flutter 허용)
├── controller/
│   ├── MapController          GET /api/v1/map/view/position
│   ├── PostController         CRUD /api/v1/posts + 댓글
│   ├── LlmController          /api/v1/llm/classify, /recommend
│   └── UsersController        /api/v1/users/info (안내용)
├── domain/
│   ├── Building               한글 DB 컬럼명 ↔ 영문 Java 필드 (@Column(name="..."))
│   ├── Post / PostCategory
│   ├── Comment                대댓글 지원 (parent_id 자기참조)
│   ├── Bookmark
│   └── dto/                   각종 요청/응답 DTO
├── repository/
│   ├── BuildingRepository     findByXBetweenAndYBetween
│   ├── PostRepository         findByCategory, findByUserId
│   ├── CommentRepository      findByPostIdAndParentIsNull
│   └── BookmarkRepository
└── service/
    ├── MapService
    ├── PostService            게시글 CRUD (작성자 검증은 Auth Server 호출)
    ├── CommentService
    ├── LlmService             FastAPI 우선 → DB 폴백 → Claude 프롬프트
    ├── UserService            AuthServerClient 위임 (DB 직접 조회 안 함)
    └── RedisUtil
```

## 핵심 설계 결정
- **Auth Server 연동**: JWT 검증은 로컬(시크릿 공유), 사용자 정보는 HTTP 호출
- **FastAPI 폴백**: FastAPI 장애시 DB 좌표범위 검색으로 자동 전환
- **Building 필드명**: Java는 영문, DB 컬럼은 기존 한글 유지 (마이그레이션 비용 절감)
- **Kafka 제거**: 결과를 클라이언트에 전달할 방법 없어 제거. 지도는 MapController로 충분

## 환경변수
```
JWT_SECRET           (최소 32자, Auth Server와 동일)
CLAUDE_API_KEY
SPRING_DATASOURCE_URL / USERNAME / PASSWORD
REDIS_HOST / REDIS_PORT
FASTAPI_BASE_URL     (기본값 http://localhost:8000)
AUTH_SERVER_BASE_URL (기본값 http://localhost:8081)
```

## 아직 없는 것
- Dockerfile (생성 필요)
- GitHub Actions CI/CD
- k8s yaml이 Kafka 기준 → 업데이트 필요
