package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RoomRecommendRequestDto {
    private String school;          // 인근 학교 (예: 한양대)
    private int budget;             // 월세 예산 (만원 단위)
    private double centerX;         // 검색 중심 경도
    private double centerY;         // 검색 중심 위도
    private double radius;          // 검색 반경
    private List<String> preferences; // 선호 편의시설 (예: ["편의점", "카페", "CCTV"])
}
