package com.capstone.Jachwi_inServerSpring.controller;

import com.capstone.Jachwi_inServerSpring.domain.dto.UserJoinDto;
import com.capstone.Jachwi_inServerSpring.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @Autowired
    ObjectMapper objectMapper; //java의 객체를 json의 객체로 만들어 주는 것.

    @Test
    @DisplayName("회원가입 성공")
    void join() throws Exception {
        String email = "2@gmail.com";
        String name = "용우";
        String password = "1234";
        String nickname = "자르반2세";
        String school = "서울과기대";

        mockMvc.perform(post("/api/v1/users/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new UserJoinDto(email, name, password, nickname, school)))) //http request에 값을 보낼땐 byte로 값을 보낸다.
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원가입 성공")
    void join_fail() throws Exception {
        String email = "2@gmail.com";
        String name = "용우";
        String password = "1234";
        String nickname = "자르반2세";
        String school = "서울과기대";

        mockMvc.perform(post("/api/v1/users/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new UserJoinDto(email, name, password, nickname, school)))) //http request에 값을 보낼땐 byte로 값을 보낸다.
                .andDo(print())
                .andExpect(status().isConflict());
    }
}