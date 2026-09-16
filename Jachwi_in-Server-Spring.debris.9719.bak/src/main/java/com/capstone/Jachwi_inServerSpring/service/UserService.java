package com.capstone.Jachwi_inServerSpring.service;

import com.capstone.Jachwi_inServerSpring.client.AuthServerClient;
import com.capstone.Jachwi_inServerSpring.client.UserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthServerClient authServerClient;

    /**
     * JWT에서 꺼낸 email로 Auth Server에서 사용자 정보 조회
     * (Main Server는 users 테이블에 직접 접근하지 않음)
     */
    public UserInfoDto getUserByEmail(String email) {
        return authServerClient.getUserByEmail(email);
    }
}
