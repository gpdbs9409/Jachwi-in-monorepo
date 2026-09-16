package com.capstone.Jachwi_inServerSpring.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.capstone.Jachwi_inServerSpring.client.FastApiClient;
import com.capstone.Jachwi_inServerSpring.client.FastApiSearchResponse;
import com.capstone.Jachwi_inServerSpring.domain.Building;
import com.capstone.Jachwi_inServerSpring.domain.dto.ChatMessageDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.ChatRecommendRequestDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.ChatRecommendResponseDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.PostClassifyRequestDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.RecommendedBuildingDto;
import com.capstone.Jachwi_inServerSpring.domain.dto.RoomRecommendRequestDto;
import com.capstone.Jachwi_inServerSpring.util.InputSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final AnthropicClient anthropicClient;
    private final MapService mapService;
    private final FastApiClient fastApiClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CLASSIFY_CACHE_PREFIX = "llm:classify:";
    private static final String RECOMMEND_CACHE_PREFIX = "llm:recommend:";
    private static final String CHAT_CACHE_PREFIX = "llm:chat:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    // 채팅 추천은 지도 뷰포트에 종속적이라(계속 움직임) 짧게만 캐시
    private static final Duration CHAT_CACHE_TTL = Duration.ofMinutes(10);
    private static final int MAX_CANDIDATES_TO_LLM = 5;

    // ─────────────────────────────────────────────
    // 1. 게시글 분류
    // ─────────────────────────────────────────────
    public String classifyPost(PostClassifyRequestDto dto) {
        String title = InputSanitizer.sanitize(dto.getTitle());
        String content = InputSanitizer.sanitize(dto.getContent());
        String cacheKey = CLASSIFY_CACHE_PREFIX + md5(title + content);
        String cached = getCache(cacheKey);
        if (cached != null) {
            log.info("[LLM 캐시 히트] 게시글 분류");
            return cached;
        }

        String prompt = """
                아래 자취 커뮤니티 게시글을 읽고, 다음 카테고리 중 하나만 골라서 카테고리 이름만 답해줘. 다른 말은 하지 마.
                카테고리: 질문, 후기, 자취팁, 정보공유, 기타

                %s
                %s
                """.formatted(
                        InputSanitizer.wrap("제목", title),
                        InputSanitizer.wrap("내용", content));

        String result = callClaude(prompt, 50, Model.CLAUDE_HAIKU_4_5, "classify");
        saveCache(cacheKey, result, CACHE_TTL);
        return result;
    }

    // ─────────────────────────────────────────────
    // 2. 자취방 추천 (기존 수동 폼 기반 — /recommend)
    // ─────────────────────────────────────────────
    public String recommendRooms(RoomRecommendRequestDto dto) {
        String school = InputSanitizer.sanitize(dto.getSchool());
        InputSanitizer.sanitizeAll(dto.getPreferences()); // 위험 패턴 감지 시 예외 발생
        String cacheKey = RECOMMEND_CACHE_PREFIX + md5(dto.toString());
        String cached = getCache(cacheKey);
        if (cached != null) {
            log.info("[LLM 캐시 히트] 자취방 추천");
            return cached;
        }

        // 1순위: FastAPI 벡터 검색 (사용자 조건 자연어 → 유사 건물)
        String searchQuery = buildSearchQuery(dto);
        long t0 = System.currentTimeMillis();
        List<Map<String, Object>> vectorBuildings = fastApiClient.searchBuildings(searchQuery, 10);
        log.info("[LLM][perf] FastAPI 벡터검색 {}ms, 결과 {}건", System.currentTimeMillis() - t0, vectorBuildings.size());

        // FastAPI 장애/데이터없음 시 DB 좌표 범위 검색으로 폴백
        List<Map<String, Object>> candidates;
        if (!vectorBuildings.isEmpty()) {
            candidates = vectorBuildings;
        } else {
            log.info("[LLM] FastAPI 폴백 — DB 좌표 범위 검색 사용");
            List<Building> dbBuildings = mapService.getBuildingsInArea(
                    dto.getCenterX() - dto.getRadius(), dto.getCenterX() + dto.getRadius(),
                    dto.getCenterY() - dto.getRadius(), dto.getCenterY() + dto.getRadius()
            );
            if (dbBuildings.isEmpty()) {
                return "해당 지역에서 검색된 매물이 없습니다.";
            }
            candidates = dbBuildings.stream()
                    .sorted(Comparator.comparingDouble(
                            b -> -heuristicScore(b, dto.getPreferences())))
                    .limit(10)
                    .map(LlmService::buildingToMap)
                    .toList();
        }

        if (candidates.isEmpty()) {
            return "해당 지역에서 검색된 매물이 없습니다.";
        }

        String prefs = dto.getPreferences() == null || dto.getPreferences().isEmpty()
                ? "특별한 선호 없음"
                : String.join(", ", dto.getPreferences());
        String budgetText = dto.getBudget() > 0 ? dto.getBudget() + "만원" : "제한 없음";

        String prompt = """
                자취방을 구하는 학생을 위한 추천을 해줘.

                %s
                %s
                %s

                건물목록(%s):
                %s

                주의: 건물목록에는 임대료 정보가 없다. 예산은 참고만 하고, 실제 임대료를 지어내지 마.
                조건에 가장 잘 맞는 건물 3곳을 추천하고, 각 건물마다 추천 이유를 한 줄로 설명해줘.
                """.formatted(
                        InputSanitizer.wrap("학교", school),
                        InputSanitizer.wrap("월세예산", budgetText),
                        InputSanitizer.wrap("선호시설", prefs),
                        BUILDING_HEADER, toBuildingRows(candidates));

        String result = callClaude(prompt, 1024, Model.CLAUDE_SONNET_4_6, "recommend");
        saveCache(cacheKey, result, CACHE_TTL);
        return result;
    }

    // ─────────────────────────────────────────────
    // 3. /map 페이지 자연어 채팅 추천 (신규)
    // ─────────────────────────────────────────────
    public ChatRecommendResponseDto chatRecommend(ChatRecommendRequestDto dto) {
        String message = InputSanitizer.sanitize(dto.getMessage());
        List<ChatMessageDto> history = dto.getHistory() == null ? List.of() : dto.getHistory();
        history.forEach(h -> InputSanitizer.sanitize(h.getContent()));

        String cacheKey = CHAT_CACHE_PREFIX + md5(message + dto.getMinX() + dto.getMaxX()
                + dto.getMinY() + dto.getMaxY() + historyKey(history));
        String cachedJson = getCache(cacheKey);
        if (cachedJson != null) {
            try {
                log.info("[LLM 캐시 히트] 채팅 추천");
                return objectMapper.readValue(cachedJson, ChatRecommendResponseDto.class);
            } catch (Exception ignored) {
                // 캐시 역직렬화 실패 시 무시하고 새로 계산
            }
        }

        // 1) 자연어 → 구조화 조건 파싱 (Haiku, 저비용)
        ParsedConditions parsed = parseConditions(message, history);
        log.info("[LLM] 파싱된 조건: budget={}, preferences={}, mood={}",
                parsed.budget(), parsed.preferences(), parsed.mood());

        // 2) 벡터 검색 — 위치는 현재 지도 뷰포트로 강제, 벡터는 선호/분위기만 좁히는 데 사용
        String searchQuery = buildChatSearchQuery(message, parsed);
        long t0 = System.currentTimeMillis();
        List<FastApiSearchResponse.SearchResult> vectorResults = fastApiClient.searchWithScore(
                searchQuery, MAX_CANDIDATES_TO_LLM * 3,
                dto.getMinX(), dto.getMaxX(), dto.getMinY(), dto.getMaxY());
        log.info("[LLM][perf] FastAPI 벡터검색(뷰포트 필터) {}ms, 결과 {}건",
                System.currentTimeMillis() - t0, vectorResults.size());

        boolean usedVectorSearch = !vectorResults.isEmpty();
        List<RecommendedBuildingDto> ranked;

        if (usedVectorSearch) {
            ranked = vectorResults.stream()
                    .sorted(Comparator.comparingDouble(FastApiSearchResponse.SearchResult::getScore).reversed())
                    .limit(MAX_CANDIDATES_TO_LLM)
                    .map(r -> mapToRecommendedDto(r.getBuilding(), r.getScore()))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } else {
            log.info("[LLM] FastAPI 폴백 — DB 뷰포트 범위 + 휴리스틱 점수 사용");
            List<Building> dbBuildings = mapService.getBuildingsInArea(
                    dto.getMinX(), dto.getMaxX(), dto.getMinY(), dto.getMaxY());
            ranked = dbBuildings.stream()
                    .sorted(Comparator.comparingDouble(
                            (Building b) -> heuristicScore(b, parsed.preferences())).reversed())
                    .limit(MAX_CANDIDATES_TO_LLM)
                    .map(b -> new RecommendedBuildingDto(
                            b.getId(), b.getX(), b.getY(), buildingAddress(b),
                            normalizedHeuristicScore(b, parsed.preferences()), null))
                    .toList();
        }

        if (ranked.isEmpty()) {
            ChatRecommendResponseDto empty = new ChatRecommendResponseDto(
                    "지금 지도에 보이는 영역에서는 조건에 맞는 건물을 찾지 못했어요. 지도를 조금 넓히거나 다른 지역을 보여주세요.",
                    List.of(), usedVectorSearch);
            return empty;
        }

        // 3) 최종 자연어 답변 생성 (Sonnet) — 후보 + 대화 맥락을 함께 전달
        String reply = generateChatReply(message, history, parsed, ranked);

        // 건물별 한 줄 이유를 답변 텍스트에서 다시 뽑기보다, 안정성을 위해
        // Sonnet에게 각 건물 이유를 JSON으로도 함께 요청하는 대신 reply 자체에 녹여서 보여준다.
        ChatRecommendResponseDto result = new ChatRecommendResponseDto(reply, ranked, usedVectorSearch);

        try {
            saveCache(cacheKey, objectMapper.writeValueAsString(result), CHAT_CACHE_TTL);
        } catch (Exception e) {
            log.warn("[LLM] 채팅 추천 캐시 저장 실패: {}", e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────
    // 조건 파싱 (Haiku)
    // ─────────────────────────────────────────────
    private record ParsedConditions(Integer budget, List<String> preferences, String mood) {}

    private ParsedConditions parseConditions(String message, List<ChatMessageDto> history) {
        String historyText = history.isEmpty() ? "(없음)" : history.stream()
                .map(h -> "%s: %s".formatted(h.getRole(), h.getContent()))
                .collect(Collectors.joining("\n"));

        String prompt = """
                아래는 자취방을 찾는 사용자와의 대화야. 마지막 사용자 발화에서 조건을 뽑아서
                반드시 아래 JSON 형식으로만 답해. 다른 설명은 절대 붙이지 마.

                {"budget": 숫자 또는 null, "preferences": ["편의점","카페","CCTV","가로등","병원","식당","버스정류장" 중 언급된 것들], "mood": "조용함/가성비/안전 등 짧은 요약, 없으면 빈 문자열"}

                %s
                %s
                """.formatted(
                        InputSanitizer.wrap("이전대화", historyText),
                        InputSanitizer.wrap("이번발화", message));

        String raw = callClaude(prompt, 200, Model.CLAUDE_HAIKU_4_5, "chat-parse");
        return parseConditionsJson(raw);
    }

    private ParsedConditions parseConditionsJson(String raw) {
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end < 0 || end < start) throw new IllegalStateException("no json");
            JsonNode node = objectMapper.readTree(raw.substring(start, end + 1));

            Integer budget = node.hasNonNull("budget") ? node.get("budget").asInt() : null;
            List<String> preferences = new ArrayList<>();
            if (node.has("preferences") && node.get("preferences").isArray()) {
                node.get("preferences").forEach(p -> preferences.add(p.asText()));
            }
            String mood = node.hasNonNull("mood") ? node.get("mood").asText() : "";
            return new ParsedConditions(budget, preferences, mood);
        } catch (Exception e) {
            log.warn("[LLM] 조건 파싱 JSON 실패, 원문 검색어로 대체: {}", e.getMessage());
            return new ParsedConditions(null, List.of(), "");
        }
    }

    private String buildChatSearchQuery(String message, ParsedConditions parsed) {
        StringBuilder sb = new StringBuilder(message);
        if (!parsed.preferences().isEmpty()) {
            sb.append(" 선호시설: ").append(String.join(", ", parsed.preferences()));
        }
        if (parsed.mood() != null && !parsed.mood().isBlank()) {
            sb.append(" 분위기: ").append(parsed.mood());
        }
        return sb.toString();
    }

    private String generateChatReply(String message, List<ChatMessageDto> history,
                                      ParsedConditions parsed, List<RecommendedBuildingDto> candidates) {
        String historyText = history.isEmpty() ? "(없음)" : history.stream()
                .map(h -> "%s: %s".formatted(h.getRole(), h.getContent()))
                .collect(Collectors.joining("\n"));

        String candidateRows = candidates.stream()
                .map(c -> "%s|score=%.2f".formatted(c.getAddress(), c.getScore()))
                .collect(Collectors.joining("\n"));

        String budgetText = parsed.budget() != null ? parsed.budget() + "만원" : "언급 없음";

        String prompt = """
                자취방을 찾는 학생과 채팅 중이야. 사용자가 지금 지도에서 보고 있는 영역 안의
                건물 후보 목록을 줄 테니, 대화 맥락과 조건에 맞춰 자연스럽게 답해줘.

                %s
                %s
                %s
                %s

                후보 건물 목록(주소|유사도점수, 점수가 높을수록 조건에 더 잘 맞음):
                %s

                규칙:
                - 후보 중 2~4곳을 골라 추천하고, 각각 왜 좋은지 한 줄로 설명해.
                - 임대료 데이터는 없으니 예산은 참고만 하고 구체적인 임대료 숫자를 지어내지 마.
                - 존댓말로, 채팅창에 어울리게 3~6문장 내외로 답해.
                """.formatted(
                        InputSanitizer.wrap("이전대화", historyText),
                        InputSanitizer.wrap("이번질문", message),
                        InputSanitizer.wrap("예산", budgetText),
                        InputSanitizer.wrap("선호시설", parsed.preferences().isEmpty() ? "없음" : String.join(", ", parsed.preferences())),
                        candidateRows);

        return callClaude(prompt, 600, Model.CLAUDE_SONNET_4_6, "chat-reply");
    }

    // ─────────────────────────────────────────────
    // 건물 payload/엔티티 ↔ DTO 변환 및 휴리스틱 점수
    // ─────────────────────────────────────────────
    private static final String BUILDING_HEADER = "주소|편의점|카페|CCTV|버스정류장|학교거리m|가로등";

    private static Map<String, Object> buildingToMap(Building b) {
        return Map.ofEntries(
                Map.entry("id", b.getId()),
                Map.entry("x", b.getX()),
                Map.entry("y", b.getY()),
                Map.entry("시도명", nz(b.getProvince())),
                Map.entry("시군구", nz(b.getDistrict())),
                Map.entry("도로명", nz(b.getStreetName())),
                Map.entry("편의점", nzInt(b.getConvenienceStore())),
                Map.entry("카페", nzInt(b.getCafe())),
                Map.entry("CCTV", nzInt(b.getCctv())),
                Map.entry("버스정류장", nzInt(b.getBusStop())),
                Map.entry("학교_거리", b.getSchoolDistance() != null ? b.getSchoolDistance() : 0.0),
                Map.entry("가로등", nzInt(b.getStreetLight()))
        );
    }

    private static String toBuildingRows(List<Map<String, Object>> candidates) {
        return candidates.stream()
                .map(b -> "%s %s %s|%s|%s|%s|%s|%s|%s".formatted(
                        b.getOrDefault("시도명", ""), b.getOrDefault("시군구", ""), b.getOrDefault("도로명", ""),
                        b.getOrDefault("편의점", 0), b.getOrDefault("카페", 0), b.getOrDefault("CCTV", 0),
                        b.getOrDefault("버스정류장", 0), b.getOrDefault("학교_거리", 0), b.getOrDefault("가로등", 0)
                ))
                .collect(Collectors.joining("\n"));
    }

    /** Qdrant payload(Map) → 지도 하이라이트용 DTO. x/y/id가 없으면 지도에 표시할 수 없으므로 null 반환 후 필터링. */
    private RecommendedBuildingDto mapToRecommendedDto(Map<String, Object> payload, double score) {
        if (payload == null) return null;
        Object xObj = payload.get("x");
        Object yObj = payload.get("y");
        if (xObj == null || yObj == null) return null;
        Long id = payload.get("id") != null ? ((Number) payload.get("id")).longValue() : null;
        double x = ((Number) xObj).doubleValue();
        double y = ((Number) yObj).doubleValue();
        String address = "%s %s %s".formatted(
                str(payload.get("시도명")), str(payload.get("시군구")), str(payload.get("도로명"))).trim();
        return new RecommendedBuildingDto(id, x, y, address, score, null);
    }

    private String buildingAddress(Building b) {
        return "%s %s %s".formatted(nz(b.getProvince()), nz(b.getDistrict()), nz(b.getStreetName())).trim();
    }

    /** 벡터검색이 불가능할 때(=Qdrant 미색인/장애) 쓰는 대체 랭킹. 언급된 선호시설의 보유 수를 단순 합산. */
    private double heuristicScore(Building b, List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            // 선호 조건이 없으면 전체 편의시설 총합으로 대략적인 "살기 좋은 정도"를 근사
            return nzInt(b.getConvenienceStore()) + nzInt(b.getCafe()) + nzInt(b.getCctv())
                    + nzInt(b.getBusStop()) + nzInt(b.getStreetLight()) + nzInt(b.getHospital())
                    + nzInt(b.getRestaurant());
        }
        double score = 0;
        for (String p : preferences) {
            score += switch (p) {
                case "편의점" -> nzInt(b.getConvenienceStore());
                case "카페" -> nzInt(b.getCafe());
                case "CCTV" -> nzInt(b.getCctv());
                case "가로등" -> nzInt(b.getStreetLight());
                case "병원" -> nzInt(b.getHospital());
                case "식당" -> nzInt(b.getRestaurant());
                case "버스정류장" -> nzInt(b.getBusStop());
                default -> 0;
            };
        }
        return score;
    }

    /** heuristicScore를 0~1 범위로 대충 눌러서, 벡터 유사도 점수와 같은 스케일 감각으로 프론트에 보여주기 위함. */
    private double normalizedHeuristicScore(Building b, List<String> preferences) {
        double raw = heuristicScore(b, preferences);
        return raw <= 0 ? 0.0 : Math.min(1.0, raw / 10.0);
    }

    private static int nzInt(Integer i) { return i == null ? 0 : i; }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private String historyKey(List<ChatMessageDto> history) {
        return history.stream().map(h -> h.getRole() + ":" + h.getContent()).collect(Collectors.joining("|"));
    }

    // ─────────────────────────────────────────────
    // FastAPI 검색 쿼리 생성 (기존 수동 폼 /recommend용)
    // ─────────────────────────────────────────────
    private String buildSearchQuery(RoomRecommendRequestDto dto) {
        String prefs = (dto.getPreferences() == null || dto.getPreferences().isEmpty())
                ? "편의시설 무관"
                : String.join(", ", dto.getPreferences());
        return "%s 근처 자취방, 선호시설: %s".formatted(dto.getSchool(), prefs);
    }

    // ─────────────────────────────────────────────
    // Claude API 호출 공통 메서드
    // ─────────────────────────────────────────────
    private String callClaude(String userPrompt, long maxTokens, Model model, String tag) {
        long t0 = System.currentTimeMillis();
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .addUserMessage(userPrompt)
                .build();

        Message response = anthropicClient.messages().create(params);
        log.info("[LLM][perf] Claude 호출({}) {}ms", tag, System.currentTimeMillis() - t0);

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining());
    }

    // ─────────────────────────────────────────────
    // Redis 캐시 유틸
    // ─────────────────────────────────────────────
    private String getCache(String key) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    private void saveCache(String key, String value, Duration ttl) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(key, value, ttl);
    }

    private String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
