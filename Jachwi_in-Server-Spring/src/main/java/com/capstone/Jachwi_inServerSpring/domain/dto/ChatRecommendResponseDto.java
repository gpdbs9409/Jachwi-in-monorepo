package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatRecommendResponseDto {
    private String reply;                          // 채팅창에 보여줄 자연어 답변
    private List<RecommendedBuildingDto> buildings; // 지도에 하이라이트할 건물 목록
    private boolean usedVectorSearch;               // true=Qdrant 벡터검색, false=DB 폴백 (디버그/투명성용)
}
