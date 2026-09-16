-- dummy_data.sql에 원래 들어있던 5명 비밀번호 해시가 "password123"과 실제로 매칭되지
-- 않는 잘못된 값이어서(bcrypt.checkpw로 검증해서 확인함) 로그인이 항상 실패했음.
-- 아래 해시는 python bcrypt로 새로 생성해서 "password123"과 매칭되는 걸 직접 검증한 값.
UPDATE users SET password = '$2a$10$DH4RHws1MmhpNZKC0cNLMeF47.5ocRrgBN5aCU6y7xLvjKuRJ8oqe'
WHERE email IN ('kim@test.com','lee@test.com','park@test.com','choi@test.com','jung@test.com');
