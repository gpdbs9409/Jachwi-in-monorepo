# CLAUDE.md — Auth Server (Spring Boot :8081)

## 역할
JWT 발급 전담 서버. 로그인/회원가입/이메일 인증만 처리.
Main Server는 이 서버에서 발급한 토큰을 공유 시크릿으로 검증함.

## API
```
POST /auth/login                     로그인 → {accessToken(1h), refreshToken(7d)}
GET  /auth/join/mailConfirm/{email}  이메일 중복확인 + 인증코드 발송 (Redis 5분 TTL)
POST /auth/join/mailConfirm          인증코드 검증 {email, ePw}
POST /auth/join                      회원가입 {email, name, nickname, password, school}
GET  /auth/internal/users/{email}    서버 내부용: Main Server가 사용자 정보 조회
```

## 핵심 설계
- Security: `/auth/**` 전체 공개 (JWT 없어도 접근 가능)
- JWT: HS256, 시크릿은 `JWT_SECRET` 환경변수 (Main Server와 반드시 동일)
- 비밀번호: BCrypt
- 이메일 인증: Gmail SMTP → Redis에 6자리 코드 저장 (5분 TTL)

## 환경변수
```
JWT_SECRET
SPRING_DATASOURCE_URL / USERNAME / PASSWORD
REDIS_HOST / REDIS_PORT
SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD  (Gmail 앱 비밀번호)
```

## 빌드
```bash
./gradlew bootJar -x test
java -jar build/libs/*.jar
```
Dockerfile 있음 (멀티스테이지 빌드, eclipse-temurin:17)
