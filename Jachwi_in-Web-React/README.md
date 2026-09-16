# 자취인 Web (React)

Flutter 앱을 대체하는 자취인 웹 프론트엔드. Vite + React + TypeScript, 백엔드(Auth Server / Main Server)와는
순수 REST + JWT로 통신한다.

## 로컬 실행

1. 의존성 설치
   ```bash
   npm install
   ```
2. 환경변수 설정
   ```bash
   cp .env.example .env.local
   ```
   - `VITE_AUTH_API_URL` / `VITE_MAIN_API_URL` : 로컬 기본값(8081/8080)은 그대로 두면 됨
   - `VITE_NAVER_MAP_CLIENT_ID` : 네이버클라우드플랫폼 콘솔에서 **Web Dynamic Map** 서비스로 발급받은
     Client ID. (Flutter 모바일 SDK용 Client ID와는 다른 값이어야 함 — 발급 안 하면 지도 화면 대신
     안내 문구만 보임)
3. 백엔드 먼저 기동 (`Jachwi_in-Server-Spring/docker-compose.yml` 기준)
   ```bash
   cd ../Jachwi_in-Server-Spring
   docker-compose up -d mysql redis qdrant
   # Auth Server / Main Server / FastAPI는 각자 레포에서 ./gradlew bootRun, uvicorn 등으로 개별 실행해도 됨
   ```
4. 개발 서버 실행
   ```bash
   npm run dev
   ```
   http://localhost:5173 접속

## 백엔드 쪽 필요 조건

- Auth Server(8081), Main Server(8080)에 `CorsConfig`를 추가해 `http://localhost:5173`을
  허용하도록 이미 수정해뒀음 (`CORS_ALLOWED_ORIGINS` 환경변수로 배포 시 도메인 교체).
- 로그인은 `POST /auth/login`, 인증이 필요한 API는 `Authorization: Bearer <accessToken>` 헤더를 자동으로 붙임
  (`src/api/client.ts`). accessToken 만료(401) 시 refreshToken으로 한 번 자동 재시도.

## 폴더 구조

```
src/
  api/         axios 인스턴스 + 엔드포인트별 함수 (auth, map, posts, llm)
  contexts/    AuthContext (로그인 상태, JWT)
  components/  Layout, NaverMap, ProtectedRoute
  pages/       화면 단위 컴포넌트
```

## 빌드 / 배포

```bash
npm run build   # dist/ 에 정적 파일 생성 → Vercel에 그대로 임포트 가능
```
