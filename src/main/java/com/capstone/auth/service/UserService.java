package com.capstone.auth.service;

import com.capstone.auth.config.JwtUtil;
import com.capstone.auth.domain.User;
import com.capstone.auth.domain.dto.TokenDto;
import com.capstone.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String checkEmailDuplicate(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new RuntimeException(email + "는 이미 존재하는 이메일입니다.");
        });
        return "SUCCESS";
    }

    public void join(String email, String name, String nickname, String password, String school) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .nickname(nickname)
                .school(school)
                .build();
        userRepository.save(user);
    }

    public TokenDto login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        return TokenDto.builder()
                .accessToken(jwtUtil.generateAccessToken(email))
                .refreshToken(jwtUtil.generateRefreshToken(email))
                .build();
    }
}
