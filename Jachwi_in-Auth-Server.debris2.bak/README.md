# Auth Server - README

**자취인 프로젝트 인증 마이크로서비스**

---

## 개요

Auth Server는 자취인 프로젝트의 **인증(Authentication) 및 인가(Authorization)** 를 담당하는 독립적인 마이크로서비스입니다.

- **포트**: 8081
- **프레임워크**: Spring Boot 3.1.1
- **언어**: Java 17
- **데이터베이스**: MySQL
- **주요 기능**:
  - 회원가입 / 로그인
  - 이메일 인증 (6자리 인증코드)
  - JWT 토큰 발급 (AccessToken + RefreshToken)

---

## 프로젝트 구조

```
src/main/
├── java/com/capstone/auth/
│   ├── controller/
│   │   └── AuthController.java        # 인증 API 엔드포인트
│   ├── domain/
│   │   ├── User.java                  # 사용자 엔티티
│   │   └── dto/                       # DTO (LoginDto, UserJoinDto 등)
│   └── service/
│       ├── UserService.java           # 사용자 비즈니스 로직
│       └── EmailServiceImpl.java       # 이메일 발송 및 검증
├── resources/
│   ├── application.properties         # 설정
│   └── schema.sql                     # DB 초기화 스크립트
└── test/                              # 테스트 코드
```

---

## 로컬 환경 설정

### 1. 필수 설치 항목
- Java 17 이상
- MySQL 8.0 이상
- Gradle 7.x 이상

### 2. 데이터베이스 설정

```bash
# MySQL 실행
mysql -u root -p

# 데이터베이스 생성
CREATE DATABASE jachwiin_auth;
USE jachwiin_auth;

# 테이블 생성 (src/main/resources/schema.sql 실행)
SOURCE src/main/resources/schema.sql;
```

**application.properties 설정**:
```properties
server.port=8081

# MySQL 연결
spring.datasource.url=jdbc:mysql://localhost:3306/jachwiin_auth
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# 이메일 설정 (Gmail SMTP 예시)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# JWT 설정
jwt.secret=your_secret_key_here_make_it_long_and_complex
jwt.expiration=3600000
jwt.password.secret=your_password_secret
```

### 3. 서버 실행

```bash
# Gradle을 이용한 빌드 및 실행
./gradlew bootRun

# 또는 JAR 파일 생성 후 실행
./gradlew build
java -jar build/libs/auth-server.jar
```

**실행 확인**:
```bash
curl http://localhost:8081/auth/login # 405 Method Not Allowed → 정상
```

---

## API 사용 예시

### 1. 이메일 중복 확인 + 인증코드 발송

```bash
curl -X GET http://localhost:8081/auth/join/mailConfirm/test@example.com
```

**응답**:
```json
"인증코드를 발송했습니다."
```

> 사용자의 이메일로 6자리 인증코드가 발송됩니다.

---

### 2. 인증코드 검증

```bash
curl -X POST http://localhost:8081/auth/join/mailConfirm \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "ePw": "123456"
  }'
```

**응답**:
```json
"인증 완료"
```

---

### 3. 회원가입 (인증 완료 후)

```bash
curl -X POST http://localhost:8081/auth/join \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "홍길동",
    "nickname": "길동이",
    "password": "password123",
    "school": "서울대학교"
  }'
```

**응답**:
```json
"회원가입 완료"
```

---

### 4. 로그인

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**응답**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

> AccessToken은 Main Server의 인증된 API 호출 시 사용합니다.

---

## 주요 기능 설명

### 📧 이메일 인증 흐름
```
1. 사용자가 /auth/join/mailConfirm/{email} 요청
2. 서버가 이메일 중복 확인
3. Redis에 인증코드 저장 (5분 유효)
4. 사용자 이메일로 인증코드 발송
5. 사용자 입력 코드 검증 (Redis 조회)
6. 인증 완료, /auth/join 호출 가능
```

### 🔐 JWT 토큰
- **AccessToken**: 단기유효 (1시간) - API 인증용
- **RefreshToken**: 장기유효 (7일) - 토큰 갱신용

### 🔒 비밀번호 암호화
- Spring Security의 `BCryptPasswordEncoder` 사용
- 평문 저장 금지

---

## 필요한 의존성

```gradle
// Spring Security + JWT
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'

// 이메일 발송
implementation 'org.springframework.boot:spring-boot-starter-mail:3.1.1'

// 데이터베이스
implementation 'mysql:mysql-connector-java:8.0.28'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

// Redis (선택사항, 인증코드 캐싱용)
implementation 'org.springframework.boot:spring-boot-starter-data-redis'

// Lombok
compileOnly 'org.projectlombok:lombok:1.18.34'
annotationProcessor 'org.projectlombok:lombok:1.18.34'
```

---

## 개발 가이드

### Spring Security 설정

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/auth/**").permitAll()  // 인증 API는 누구나 접근 가능
                .anyRequest().authenticated()
            .and()
            .addFilter(new JwtAuthenticationFilter(authenticationManager()))
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 토큰 검증 (Main Server에서)

```java
// Main Server에서 AccessToken 검증
String token = "Bearer eyJhbGc...";
String extracted = token.replace("Bearer ", "");
Claims claims = Jwts.parserBuilder()
    .setSigningKey(secretKey)
    .build()
    .parseClaimsJws(extracted)
    .getBody();

String email = claims.getSubject();
```

---

## 주의사항 & 보안

⚠️ **프로덕션에서 필수**:

1. **JWT Secret**: 최소 32자 이상의 복잡한 문자열
2. **MySQL 비밀번호**: 강력한 비밀번호 설정
3. **Gmail 앱 비밀번호**: 진짜 Gmail 비밀번호 아님 (앱 전용 비밀번호 사용)
4. **HTTPS**: 프로덕션 환경에서는 반드시 HTTPS 사용
5. **CORS**: 신뢰할 수 있는 도메인만 화이트리스트 설정

```properties
# 프로덕션 설정 예시
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
jwt.expiration=3600000  # 1시간
server.servlet.session.timeout=30m
```

---

## 추가 개발 예정 기능

- [ ] 비밀번호 찾기/변경
- [ ] OAuth2 (구글, 카카오 로그인)
- [ ] 2FA (2-Factor Authentication)
- [ ] 토큰 블랙리스트 (로그아웃)
- [ ] 사용자 역할 (ROLE_USER, ROLE_ADMIN)

---

## 문제 해결

### "Connection refused: localhost:3306"
→ MySQL이 실행 중이지 않습니다. `mysql.server start` 또는 MySQL Workbench에서 시작하세요.

### "인증코드 발송 실패"
→ application.properties의 Gmail 설정을 확인하세요.
- Gmail 앱 비밀번호 사용 (2FA 활성화 필수)
- SMTP 설정이 올바른지 확인

### "JWT 토큰 검증 실패"
→ JWT Secret이 일치하는지 확인하세요 (Auth Server와 Main Server의 secret이 같아야 함).

---

## 참고 자료

- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/) - 토큰 디버깅 도구
- [MySQL JDBC Driver](https://dev.mysql.com/downloads/connector/j/)

---

**마지막 업데이트**: 2026-04-04  
**작성자**: 자취인 개발팀
