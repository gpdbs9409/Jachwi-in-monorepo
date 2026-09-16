package com.capstone.Jachwi_inServerSpring.controller;

import com.capstone.Jachwi_inServerSpring.domain.dto.ChatRecommendRequestDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.ChatRecommendResponseDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.PostClassifyRequestDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.RoomRecommendRequestDto;
import com.capstone.Jachwi_inServerSpring.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/llm")
public class LlmController {

    private final LlmService llmService;

    // 커뮤니티 게시글 카테고리 자동 분류
    @PostMapping("/classify")
    public ResponseEntity<String> classifyPost(@RequestBody PostClassifyRequestDto dto) {
        return ResponseEntity.ok(llmService.classifyPost(dto));
    }

    // 사용자 조건 기반 자취방 추천 (수동 폼 — 레거시, 하위호환용으로 유지)
    @PostMapping("/recommend")
    public ResponseEntity<String> recommendRooms(@RequestBody RoomRecommendRequestDto dto) {
        return ResponseEntity.ok(llmService.recommendRooms(dto));
    }

    // /map 페이지에 붙는 자연어 채팅 추천 (지도 뷰포트 기반)
    @PostMapping("/chat")
    public ResponseEntity<ChatRecommendResponseDto> chatRecommend(@RequestBody ChatRecommendRequestDto dto) {
        return ResponseEntity.ok(llmService.chatRecommend(dto));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
