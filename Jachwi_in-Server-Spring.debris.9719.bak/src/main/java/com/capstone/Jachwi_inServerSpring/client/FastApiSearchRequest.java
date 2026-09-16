package com.capstone.Jachwi_inServerSpring.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FastApiSearchRequest {
    private String query;
    private int top_k;
    // 지도 뷰포트로 후보를 제한할 때 사용 (null 허용 - 전체 검색)
    private Double min_x;
    private Double max_x;
    private Double min_y;
    private Double max_y;
}
