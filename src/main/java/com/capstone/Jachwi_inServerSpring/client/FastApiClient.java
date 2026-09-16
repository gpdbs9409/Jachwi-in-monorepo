package com.capstone.Jachwi_inServerSpring.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final RestTemplate restTemplate;

    @Value("${fastapi.base-url}")
    private String baseUrl;

    /**
     * 자연어 쿼리로 유사 건물 벡터 검색 (좌표 범위 제한 없음)
     */
    public List<Map<String, Object>> searchBuildings(String query, int topK) {
        return searchWithScore(query, topK, null, null, null, null).stream()
                .map(FastApiSearchResponse.SearchResult::getBuilding)
                .toList();
    }

    /**
     * 지도 뷰포트(좌표 범위)로 후보를 제한한 벡터 검색. score를 함께 반환한다.
     * 위치는 range filter로 정확히 강제하고, 벡터 유사도는 그 안에서
     * 선호시설/분위기 같은 정성적 조건을 좁히는 데에만 사용한다.
     *
     * FastAPI 장애 시 빈 리스트 반환 (호출부에서 DB 폴백 처리)
     */
    public List<FastApiSearchResponse.SearchResult> searchWithScore(
            String query, int topK,
            Double minX, Double maxX, Double minY, Double maxY) {
        try {
            FastApiSearchRequest request = new FastApiSearchRequest(query, topK, minX, maxX, minY, maxY);
            ResponseEntity<FastApiSearchResponse> response = restTemplate.postForEntity(
                    baseUrl + "/search",
                    request,
                    FastApiSearchResponse.class
            );

            if (response.getBody() == null || response.getBody().getResults() == null) {
                return Collections.emptyList();
            }
            return response.getBody().getResults();

        } catch (RestClientException e) {
            log.warn("[FastAPI] 벡터 검색 실패 — DB 폴백 사용. 원인: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
