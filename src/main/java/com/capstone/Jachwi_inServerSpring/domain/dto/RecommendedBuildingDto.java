package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 지도에 하이라이트로 표시할 추천 건물 1건.
 */
@Getter
@AllArgsConstructor
public class RecommendedBuildingDto {
    private Long id;          // building.id (Qdrant 후보는 없을 수 있어 null 허용)
    private double x;
    private double y;
    private String address;   // "시도명 시군구 도로명"
    private double score;     // 벡터 유사도(0~1) 또는 휴리스틱 점수. DB 폴백 시 0
    private String reason;    // 이 건물을 추천한 한 줄 이유 (Claude 생성)
}
