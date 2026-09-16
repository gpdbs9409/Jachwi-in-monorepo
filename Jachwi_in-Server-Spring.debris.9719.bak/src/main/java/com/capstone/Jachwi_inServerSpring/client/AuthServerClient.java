package com.capstone.Jachwi_inServerSpring.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServerClient {

    private final RestTemplate restTemplate;

    @Value("${auth-server.base-url}")
    private String baseUrl;

    /**
     * JWT에서 꺼낸 email로 Auth Server에 사용자 정보 요청
     * @throws IllegalArgumentException 사용자가 없을 때 (404)
     */
    public UserInfoDto getUserByEmail(String email) {
        try {
            return restTemplate.getForObject(
                    baseUrl + "/auth/internal/users/{email}",
                    UserInfoDto.class,
                    email
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: " + email);
        } catch (RestClientException e) {
            log.error("[AuthServer] 사용자 조회 실패 — email: {}, 원인: {}", email, e.getMessage());
            throw new RuntimeException("Auth Server 통신 오류", e);
        }
    }
}
