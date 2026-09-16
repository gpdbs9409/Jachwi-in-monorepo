package com.capstone.Jachwi_inServerSpring.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // CorsConfig 의 CorsConfigurationSource 빈을 사용
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 사용 → 세션 사용 안 함
                .authorizeHttpRequests(auth -> auth
                        // preflight(OPTIONS) 요청은 인증 없이 통과시켜야 CORS가 정상 동작함
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 인증 없이 접근 가능한 경로
                        .requestMatchers(
                                "/api/v1/users/join/**",
                                "/api/v1/users/login"
                        ).permitAll()
                        .requestMatchers("/api/v1/map/**").permitAll()
                        .requestMatchers("/error").permitAll()  // 컨트롤러 예외 시 내부 포워딩되는 경로, 막히면 진짜 에러가 403으로 가려짐
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/apt-trade/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
