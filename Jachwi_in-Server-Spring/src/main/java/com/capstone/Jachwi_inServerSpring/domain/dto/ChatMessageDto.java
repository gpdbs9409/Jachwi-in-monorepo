package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지도 페이지 AI 추천 채팅의 한 턴.
 * role: "user" | "assistant"
 */
@Getter
@NoArgsConstructor
public class ChatMessageDto {
    private String role;
    private String content;
}
