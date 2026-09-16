package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * /map 페이지에 붙는 자연어 추천 채팅 요청.
 * 학교/중심좌표를 직접 입력받지 않고, 사용자가 "지금 보고 있는 지도 영역"을
 * 그대로 검색 범위로 사용한다 (NaverMap의 현재 뷰포트 bounds).
 */
@Getter
@NoArgsConstructor
public class ChatRecommendRequestDto {
    private String message;              // 이번 턴 사용자 입력
    private List<ChatMessageDto> history; // 최근 대화 (프론트 상태, 서버는 무상태)

    // 지도 현재 뷰포트 (경도 x / 위도 y 범위)
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
}
