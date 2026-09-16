package com.capstone.Jachwi_inServerSpring.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 로그인/회원가입은 Auth Server (port 8081) 로 이전됨
// Main Server는 비즈니스 API만 제공 (지도, LLM 등)
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    @GetMapping("/info")
    public ResponseEntity<String> info() {
        // 로그인/회원가입 → POST http://auth-server:8081/auth/login
        return ResponseEntity.ok("인증은 Auth Server(8081)를 이용하세요.");
    }
}
