package com.capstone.Jachwi_inServerSpring.client;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class FastApiSearchResponse {
    private List<SearchResult> results;

    @Getter
    @NoArgsConstructor
    public static class SearchResult {
        private double score;
        // Qdrant payload — 한글 컬럼명 그대로 담겨옴
        private Map<String, Object> building;
    }
}
