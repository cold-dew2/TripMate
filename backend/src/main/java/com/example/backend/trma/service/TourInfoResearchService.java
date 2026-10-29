package com.example.backend.trma.service;

import com.example.backend.trma.dto.dataList.TourInfoAiData;
import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.util.AiErrorUtil;
import com.example.backend.trma.util.AiJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

// [정보 보완] TourAPI에 없는 이용정보 항목을 AI가 "조사"한다. (번역과는 별개 단계)
//
// - Gemini에 Google 검색 도구(google_search)를 붙여 실제 웹 검색 결과에 근거한 값만 받는다.
// - 항목마다 출처 URL을 요구하고, 그 URL의 도메인이 실제 검색 근거(groundingChunks)에 있을 때만
//   FOUND로 저장한다. 출처가 없거나 검색 근거와 맞지 않거나 "확인 필요" 같은 값은 버리고 NOT_FOUND로
//   저장한다(화면에 노출하지 않음). 추측값을 만들지 않는 것이 목적이다.
// - 결과는 TB_TRMA_TOUR_INFO_AI에 한국어로 저장한다(TourAPI 원본 TB_TRMA_TOUR_INTRO와 분리).
//   영어/일본어는 이 값을 원문 삼아 번역 저장소에서 따로 번역한다.
// - AI 호출 자체가 실패(한도 초과, 네트워크 등)하면 "조사 실패"로 보고 아무것도 저장하지 않는다
//   (NOT_FOUND와 구분). 대신 같은 관광지는 잠시 다시 조사하지 않는다.
//
// 갱신 정책: FOUND 90일, NOT_FOUND 14일 후 재조사. 조사 실패는 10분 뒤 재시도.
@Service
@RequiredArgsConstructor
@Slf4j
public class TourInfoResearchService {

    public static final String FOUND = "FOUND";
    public static final String NOT_FOUND = "NOT_FOUND";
    static final long FOUND_FRESH_DAYS = 90;
    static final long NOT_FOUND_FRESH_DAYS = 14;
    static final long FAIL_RETRY_MILLIS = 10 * 60_000L;

    // 조사 대상 항목(FIELD_NM) → 프롬프트/응답 키
    static final Map<String, String> FIELD_KEYS = new LinkedHashMap<>();
    static {
        FIELD_KEYS.put("INTRO_USE_TIME", "useTime");
        FIELD_KEYS.put("INTRO_REST_DATE", "restDate");
        FIELD_KEYS.put("INTRO_USE_FEE", "useFee");
        FIELD_KEYS.put("INTRO_PARKING", "parking");
        FIELD_KEYS.put("INTRO_PARKING_FEE", "parkingFee");
        FIELD_KEYS.put("INTRO_INFO_CENTER", "infoCenter");
        FIELD_KEYS.put("INTRO_HOMEPAGE", "homepage");
    }
    private static final Map<String, String> FIELD_LABELS = Map.of(
            "useTime", "운영시간", "restDate", "휴무일", "useFee", "이용요금(입장료)",
            "parking", "주차 가능 여부/시설", "parkingFee", "주차 요금", "infoCenter", "문의 전화번호",
            "homepage", "공식 홈페이지 URL");
    // 값처럼 보이지만 실제로는 "모른다"는 뜻인 응답
    private static final List<String> UNSURE = List.of("확인 필요", "확인필요", "정보 없음", "정보없음", "알 수 없", "미확인",
            "문의 바랍", "불명", "unknown", "n/a", "null");

    private final TourTranslationMapper translationMapper;
    private final TourTranslationService translationService;
    private final RestClient restClient;

    @Value("${gemini.api.url}")
    private String url;
    @Value("${gemini.api.key}")
    private String apiKey;

    // 조사 실패한 관광지 → 다시 시도해도 되는 시각
    private final Map<String, Long> failedUntil = new ConcurrentHashMap<>();
    // 테스트에서 동기 실행으로 바꿀 수 있게 둔다
    Executor executor = ForkJoinPool.commonPool();

    // 저장된 조사 결과 중 아직 유효한 것(FOUND/NOT_FOUND 모두). key: FIELD_NM
    public Map<String, TourInfoAiData> stored(String tourId) {
        Map<String, TourInfoAiData> out = new LinkedHashMap<>();
        try {
            for (TourInfoAiData row : translationMapper.selectInfoAi(tourId)) {
                if (isFresh(row)) out.put(row.getFieldNm(), row);
            }
        } catch (Exception e) {
            log.warn("AI 조사 정보 조회에 실패했습니다. tourId={}", tourId, e);
        }
        return out;
    }

    static boolean isFresh(TourInfoAiData row) {
        if (row.getCheckedDt() == null) return false;
        long days = FOUND.equals(row.getStatusCd()) ? FOUND_FRESH_DAYS : NOT_FOUND_FRESH_DAYS;
        return row.getCheckedDt().isAfter(LocalDateTime.now().minusDays(days));
    }

    // 조사가 필요한 항목을 백그라운드로 조사한다. 이미 조사 중이거나 최근 실패했으면 시작하지 않는다.
    // 시작했거나 진행 중이면 true(화면은 잠시 뒤 다시 조회).
    public boolean researchAsync(String tourId, List<String> fields) {
        if (fields == null || fields.isEmpty()) return false;
        Long until = failedUntil.get(tourId);
        if (until != null && until > System.currentTimeMillis()) return false;
        if (!translationService.tryClaim(tourId, "RESEARCH", "ko")) return true;
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    research(tourId, fields);
                } finally {
                    translationService.release(tourId, "RESEARCH", "ko");
                }
            }, executor);
        } catch (Exception e) {
            translationService.release(tourId, "RESEARCH", "ko");
            return false;
        }
        return true;
    }

    // 실제 조사. 저장한 FOUND 건수를 돌려준다(실패 시 -1).
    int research(String tourId, List<String> fields) {
        Map<String, Object> basic;
        try {
            basic = translationMapper.tourBasic(tourId);
        } catch (Exception e) {
            log.warn("정보 조사 대상 관광지 조회에 실패했습니다. tourId={}", tourId, e);
            return -1;
        }
        if (basic == null || basic.get("tourNm") == null) return -1;

        JsonNode response;
        try {
            AiErrorUtil.throwIfQuotaBlocked();
            response = restClient.post()
                    .uri(url)
                    .header("X-goog-api-key", apiKey)
                    .body(Map.of(
                            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt(basic, fields))))),
                            "tools", List.of(Map.of("google_search", Map.of()))))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            long cooldown = AiErrorUtil.quotaCooldownMillis(e);
            if (cooldown > 0) AiErrorUtil.markQuotaBlocked(cooldown);
            failedUntil.put(tourId, System.currentTimeMillis() + FAIL_RETRY_MILLIS);
            log.warn("이용정보 AI 조사에 실패했습니다(조사 실패 — 저장하지 않음). tourId={}", tourId, e);
            return -1;
        }

        List<String[]> evidence = groundingSources(response);   // [uri, title]
        Map<String, String[]> answers = parseAnswers(response); // key → [value, sourceUrl]
        int found = 0;
        for (String field : fields) {
            String key = FIELD_KEYS.get(field);
            if (key == null) continue;
            String[] answer = answers.get(key);
            TourInfoAiData row = new TourInfoAiData();
            row.setTourId(tourId);
            row.setFieldNm(field);
            String[] matched = answer == null ? null : verify(field, answer[0], answer[1], evidence);
            if (matched != null) {
                row.setStatusCd(FOUND);
                row.setInfoText(answer[0].trim());
                row.setSourceUrl(answer[1].trim());
                row.setSourceTitle(matched[1]);
                found++;
            } else {
                row.setStatusCd(NOT_FOUND);
            }
            try {
                translationMapper.upsertInfoAi(row);
            } catch (Exception e) {
                log.warn("AI 조사 결과 저장에 실패했습니다(조사 자체는 성공). tourId={}, field={}", tourId, field, e);
            }
        }
        failedUntil.remove(tourId);
        log.info("이용정보 AI 조사 완료: tourId={}, 요청 {}항목, 출처 확인 {}항목, 검색 근거 {}건", tourId, fields.size(), found, evidence.size());
        return found;
    }

    static String prompt(Map<String, Object> basic, List<String> fields) {
        StringBuilder want = new StringBuilder();
        for (String f : fields) {
            String key = FIELD_KEYS.get(f);
            if (key != null) want.append("- ").append(key).append(" : ").append(FIELD_LABELS.get(key)).append('\n');
        }
        return "다음 한국 관광지의 이용정보를 웹 검색으로 확인해 주세요.\n"
                + "관광지명: " + basic.get("tourNm") + "\n"
                + "주소: " + (basic.get("roadAddr") == null ? "" : basic.get("roadAddr")) + "\n"
                + "[확인할 항목]\n" + want
                + "[규칙]\n"
                + "1. 공식 홈페이지, 운영기관(지자체/공공기관) 페이지 등 신뢰할 수 있는 출처에서 실제로 확인한 값만 쓰세요.\n"
                + "2. 같은 이름의 다른 장소와 혼동하지 마세요. 주소가 일치하는 곳의 정보만 쓰세요.\n"
                + "3. 확인하지 못한 항목은 value와 sourceUrl을 모두 null로 두세요. 추측하거나 일반적인 값을 만들지 마세요.\n"
                + "4. value는 한국어로, 출처에 적힌 내용을 짧게 그대로 옮기세요. sourceUrl은 값을 확인한 페이지의 실제 URL입니다.\n"
                + "5. 아래 JSON만 출력하세요. 다른 설명은 쓰지 마세요.\n"
                + "{\"fields\":{\"useTime\":{\"value\":\"...\",\"sourceUrl\":\"https://...\"}}}";
    }

    static List<String[]> groundingSources(JsonNode response) {
        List<String[]> out = new ArrayList<>();
        if (response == null) return out;
        for (JsonNode chunk : response.path("candidates").path(0).path("groundingMetadata").path("groundingChunks")) {
            JsonNode web = chunk.path("web");
            if (web.isMissingNode()) continue;
            out.add(new String[]{web.path("uri").asString(""), web.path("title").asString("")});
        }
        return out;
    }

    static Map<String, String[]> parseAnswers(JsonNode response) {
        Map<String, String[]> out = new LinkedHashMap<>();
        if (response == null) return out;
        StringBuilder text = new StringBuilder();
        for (JsonNode part : response.path("candidates").path(0).path("content").path("parts")) {
            text.append(part.path("text").asString(""));
        }
        try {
            JsonNode fields = new ObjectMapper().readTree(AiJsonUtil.extractJson(text.toString())).path("fields");
            for (String key : FIELD_KEYS.values()) {
                JsonNode f = fields.path(key);
                if (f.isMissingNode() || f.isNull()) continue;
                String value = f.path("value").isNull() ? null : f.path("value").asString(null);
                String src = f.path("sourceUrl").isNull() ? null : f.path("sourceUrl").asString(null);
                out.put(key, new String[]{value, src});
            }
        } catch (Exception e) {
            log.warn("이용정보 AI 조사 응답을 해석하지 못했습니다(모두 미확인 처리).", e);
        }
        return out;
    }

    // 값과 출처가 검증되면 일치한 검색 근거 [uri, title]를, 아니면 null.
    static String[] verify(String field, String value, String sourceUrl, List<String[]> evidence) {
        if (value == null || value.isBlank() || sourceUrl == null || sourceUrl.isBlank()) return null;
        String lower = value.toLowerCase(Locale.ROOT);
        for (String u : UNSURE) if (lower.contains(u)) return null;
        String host = host(sourceUrl);
        if (host == null) return null;
        if ("INTRO_HOMEPAGE".equals(field) && host(value) == null) return null;
        if (evidence.isEmpty()) return null; // 실제 검색 없이 만든 답은 믿지 않는다
        for (String[] e : evidence) {
            String title = e[1] == null ? "" : e[1].toLowerCase(Locale.ROOT).trim();
            if (title.startsWith("www.")) title = title.substring(4);
            String evHost = host(e[0]);
            if (!title.isEmpty() && (host.equals(title) || host.endsWith("." + title) || title.endsWith("." + host))) return e;
            // 근거 URI가 리다이렉트가 아닌 실제 주소로 온 경우
            if (evHost != null && !evHost.contains("vertexaisearch") && (host.equals(evHost) || host.endsWith("." + evHost))) return e;
        }
        return null;
    }

    static String host(String url) {
        try {
            String u = url.trim();
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                if (!u.startsWith("www.")) return null;
                u = "https://" + u;
            }
            String h = URI.create(u).getHost();
            if (h == null || !h.contains(".")) return null;
            h = h.toLowerCase(Locale.ROOT);
            return h.startsWith("www.") ? h.substring(4) : h;
        } catch (Exception e) {
            return null;
        }
    }
}
