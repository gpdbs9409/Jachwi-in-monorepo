# Auth Server API 명세서

**서버**: `http://localhost:8081`  
**설명**: 자취인 프로젝트의 인증 관련 API (회원가입, 로그인, 이메일 인증)

---

## 1. 로그인

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/auth/login` |
| **설명** | 이메일과 비밀번호로 로그인, AccessToken + RefreshToken 반환 |

### Request
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Response (200 OK)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

### Error
- **400**: 이메일 또는 비밀번호 오류
- **404**: 존재하지 않는 사용자

---

## 2. 회원가입

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/auth/join` |
| **설명** | 새로운 사용자 계정 생성 (이메일 인증 완료 후 호출) |

### Request
```json
{
  "email": "user@example.com",
  "name": "홍길동",
  "nickname": "길동이",
  "password": "password123",
  "school": "서울대학교"
}
```

### Response (200 OK)
```json
"회원가입 완료"
```

### Error
- **400**: 필수 필드 누락 또는 유효성 검증 실패
- **409**: 이미 가입된 이메일

---

## 3. 이메일 중복 확인 + 인증코드 발송

| 항목 | 값 |
|---|---|
| **요청 방식** | `GET` |
| **엔드포인트** | `/auth/join/mailConfirm/{email}` |
| **설명** | 이메일 중복성 확인 후, 인증코드를 이메일로 발송 |

### Request
```
GET /auth/join/mailConfirm/user@example.com
```

### Response (200 OK)
```json
"인증코드를 발송했습니다."
```

### Error
- **409**: 이미 가입된 이메일
- **500**: 이메일 발송 실패

---

## 4. 인증코드 검증

| 항목 | 값 |
|---|---|
| **요청 방식** | `POST` |
| **엔드포인트** | `/auth/join/mailConfirm` |
| **설명** | 사용자가 입력한 인증코드 검증 |

### Request
```json
{
  "email": "user@example.com",
  "ePw": "123456"
}
```

### Response (200 OK)
```json
"인증 완료"
```

### Response (400 Bad Request)
```json
"인증 실패"
```

---

## 미구현 기능 (TODO)

- [ ] 비밀번호 변경 (`POST /auth/password/change`)
- [ ] 비밀번호 초기화 (`POST /auth/password/reset`)
- [ ] 토큰 갱신 (`POST /auth/refresh`)
- [ ] 로그아웃 (`POST /auth/logout`)
- [ ] 계정 탈퇴 (`DELETE /auth/withdraw`)
