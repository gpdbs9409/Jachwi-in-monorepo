package com.capstone.auth.controller;

import com.capstone.auth.domain.dto.EmailAuthDto;
import com.capstone.auth.domain.dto.LoginDto;
import com.capstone.auth.domain.dto.TokenDto;
import com.capstone.auth.domain.dto.UserInfoDto;
import com.capstone.auth.domain.dto.UserJoinDto;
import com.capstone.auth.service.UserService;
import com.capstone.auth.service.impl.EmailServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final EmailServiceImpl emailService;

    // 로그인 → AccessToken + RefreshToken 반환
    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody LoginDto dto) {
        return ResponseEntity.ok(userService.login(dto.getEmail(), dto.getPassword()));
    }

    // 회원가입
    @PostMapping("/join")
    public ResponseEntity<String> join(@RequestBody UserJoinDto dto) {
        userService.join(dto.getEmail(), dto.getName(), dto.getNickname(), dto.getPassword(), dto.getSchool());
        return ResponseEntity.ok("회원가입 완료");
    }

    // 이메일 중복 확인 + 인증코드 발송
    @GetMapping("/join/mailConfirm/{email}")
    public ResponseEntity<String> mailConfirm(@PathVariable String email) throws Exception {
        userService.checkEmailDuplicate(email);
        emailService.sendSimpleMessage(email);
        return ResponseEntity.ok("인증코드를 발송했습니다.");
    }

    // 인증코드 검증
    @PostMapping("/join/mailConfirm")
    public ResponseEntity<String> verifyCode(@RequestBody EmailAuthDto dto) {
        if (emailService.verifyEmailCode(dto.getEmail(), dto.getePw())) {
            return ResponseEntity.ok("인증 완료");
        }
        return ResponseEntity.badRequest().body("인증 실패");
    }

    // 서버 내부 전용 — Main Server가 JWT에서 꺼낸 email로 사용자 정보 조회
    @GetMapping("/internal/users/{email}")
    public ResponseEntity<UserInfoDto> getUserInfo(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }
}
