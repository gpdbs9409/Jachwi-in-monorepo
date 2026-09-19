package com.capstone.Jachwi_inServerSpring.controller;

import com.capstone.Jachwi_inServerSpring.config.JwtUtil;
import com.capstone.Jachwi_inServerSpring.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Registration moved to Auth Server; verify the current Main Server contract.
@WebMvcTest(UsersController.class)
@Import(SecurityConfig.class)
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean JwtUtil jwtUtil;

    @Test @WithMockUser
    void pointsUsersToAuthServer() throws Exception {
        mockMvc.perform(get("/api/v1/users/info"))
                .andExpect(status().isOk())
                .andExpect(content().string("인증은 Auth Server(8081)를 이용하세요."));
    }

    @Test @WithMockUser
    void registrationIsNoLongerExposedByMainServer() throws Exception {
        mockMvc.perform(post("/api/v1/users/join"))
                .andExpect(status().isNotFound());
    }
}
