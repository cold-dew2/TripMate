package com.example.backend.trma.service;

import com.example.backend.trma.dto.dataList.TourAiDetailData;
import com.example.backend.trma.dto.dataList.TourInfoAiData;
import com.example.backend.trma.dto.dataList.TourIntroData;
import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.util.AiJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 관광지 이용 정보(운영시간/휴무일/요금/주차/문의/홈페이지)를 한국관광공사 TourAPI에서 가져온다.
//
// 예전에는 이 정보를 Gemini에게 "알려 달라"고 물어 지어낸 값이 섞였다(엉뚱한 홈페이지, 무료/유료 오표시).
// 이제는 TourAPI detailIntro2(소개정보) + detailCommon2(전화/홈페이지)가 준 값만 쓰고,
// API에 없는 항목은 TourInfoResearchService가 신뢰할 수 있는 출처(검색 근거 + 출처 URL)로 확인한
// 값만 보완하고(출처를 못 찾으면 숨김), AI 번역은 그와 별개 단계로 국문 값을 영어/일본어로 옮긴다.
//
// 언어별 우선순위
//   ko : KorService2 원문
//   en/ja : 같은 관광지의 EngService2/JpnService2 공식 값(언어 매칭된 경우)
//           → 저장된 번역(MANUAL > TOURAPI > LEGACY > AI, 원문 해시 일치분만)
//           → 새 AI 번역(검증 후 저장) → 실패 시 국문 원문
// 조회 결과는 TB_TRMA_TOUR_INTRO에 캐시해 같은 관광지는 API를 다시 부르지 않는다.
@Service
@RequiredArgsConstructor
@Slf4j
public class TourIntroService {

    // 이용 정보는 자주 바뀌지 않으므로 30일 동안 캐시를 그대로 쓴다. 조회 실패는 10분 뒤 다시 시도한다.
    static final long FRESH_DAYS = 30;
    static final long FAIL_RETRY_MINUTES = 10;

    public static final String SOURCE_TOURAPI = "TOURAPI";              // 해당 언어 공식 데이터
    public static final String SOURCE_TRANSLATED = "TOURAPI_TRANSLATED"; // 국문 공식 데이터를 번역
    public static final String SOURCE_ORIGINAL = "TOURAPI_KO";           // 번역이 없어 국문 원문 표시
    public static final String SOURCE_NONE = "NONE";                     // TourAPI에 정보 없음

    private final TourTranslationMapper translationMapper;
    private final TourTranslationService translationService;
    private final RestClient restClient;
    private final TourInfoResearchService researchService;

    @Value("${tour.api.host}")
    private String host;
    @Value("${tour.api.key}")
    private String apiKey;
    @Value("${tour.api.KorService.detailCommon.path}")
    private String korCommonPath;
    @Value("${tour.api.EngService.detailCommon.path}")
    private String engCommonPath;
    @Value("${tour.api.JpnService.detailCommon.path}")
    private String jpnCommonPath;

    // 화면 항목명 ↔ 저장 FIELD_NM
    static final Map<String, String> RESPONSE_KEYS = new LinkedHashMap<>();
    static {
        RESPONSE_KEYS.put("INTRO_USE_TIME", "operatingHours");
        RESPONSE_KEYS.put("INTRO_REST_DATE", "closedDays");
        RESPONSE_KEYS.put("INTRO_USE_FEE", "admissionFeeDetails");
        RESPONSE_KEYS.put("INTRO_PARKING", "parkingInfo");
        RESPONSE_KEYS.put("INTRO_PARKING_FEE", "parkingFeeInfo");
        RESPONSE_KEYS.put("INTRO_INFO_CENTER", "infoCenter");
        RESPONSE_KEYS.put("INTRO_TEL", "tel");
        RESPONSE_KEYS.put("INTRO_HOMEPAGE", "websiteUrl");
    }
    // 번역하지 않는 항목(URL/전화번호)
    private static final List<String> NO_TRANSLATE = List.of("INTRO_HOMEPAGE", "INTRO_TEL");

    public static final String ORIGIN_TOURAPI = "TOURAPI";
    public static final String ORIGIN_AI_RESEARCH = "AI_RESEARCH";

    // translator: (key → 국문 텍스트, lang) → (key → 번역문). Gemini 호출은 기존 번역 함수를 재사용한다.
    //
    // 처리 단계
    //  A. 정보 보완(국문 기준): TourAPI 국문 값 → 저장된 AI 조사값(FOUND, 출처 있음) → 없으면 AI 조사 예약
    //     (NOT_FOUND는 "신뢰할 출처 없음"으로 숨김). TourAPI 값이 있으면 조사값으로 대체하지 않는다.
    //  B. 번역 보완(en/ja): 해당 언어 TourAPI 공식 값 → 저장된 번역(MANUAL > TOURAPI > LEGACY > AI, 원문 해시 일치)
    //     → 새 AI 번역(검증 후 영구 저장) → 실패 시 국문 원문
    //  A가 만든 국문 값도 B에서 똑같이 번역한다(조사값을 언어별로 다시 조사하지 않는다).
    public TourAiDetailData usageInfo(String tourId, String lang,
                                      BiFunction<Map<String, String>, String, Map<String, String>> translator) {
        String langCd = "en".equals(lang) || "ja".equals(lang) ? lang : "ko";

        // ===== A. 정보 보완 (국문) =====
        TourIntroData ko = load(tourId, "ko", tourId);
        Map<String, String> koVals = ko == null ? new LinkedHashMap<>() : allFields(ko);
        Map<String, TourAiDetailData.FieldInfo> meta = new LinkedHashMap<>();
        koVals.keySet().forEach(f -> meta.put(f, info(ORIGIN_TOURAPI)));

        boolean pending = false;
        // TourAPI 조회 자체가 실패했으면(ko == null) 무엇이 빠졌는지 모르므로 조사하지 않는다.
        if (ko != null && researchService != null) {
            Map<String, TourInfoAiData> researched = researchService.stored(tourId);
            List<String> toResearch = new ArrayList<>();
            for (String field : TourInfoResearchService.FIELD_KEYS.keySet()) {
                if (koVals.containsKey(field)) continue;              // TourAPI 원본 우선
                TourInfoAiData r = researched.get(field);
                if (r == null) {
                    toResearch.add(field);
                } else if (TourInfoResearchService.FOUND.equals(r.getStatusCd()) && !blank(r.getInfoText())) {
                    koVals.put(field, r.getInfoText());
                    TourAiDetailData.FieldInfo fi = info(ORIGIN_AI_RESEARCH);
                    fi.setSourceUrl(r.getSourceUrl());
                    fi.setSourceTitle(r.getSourceTitle());
                    fi.setCheckedAt(r.getCheckedDt() == null ? null : r.getCheckedDt().toString());
                    meta.put(field, fi);
                }
                // NOT_FOUND: 신뢰할 수 있는 출처를 찾지 못함 → 노출하지 않음
            }
            pending = researchService.researchAsync(tourId, toResearch);
        }

        Map<String, String> resolved = new LinkedHashMap<>();
        if ("ko".equals(langCd)) {
            resolved.putAll(koVals);
            meta.values().forEach(fi -> fi.setTranslation("NONE"));
        } else {
            // ===== B. 번역 보완 (en/ja) =====
            TourIntroData official = null;
            String langContentId = safeLangContentId(tourId, langCd);
            if (langContentId != null) official = load(tourId, langCd, langContentId);
            Map<String, String> officialVals = official == null ? Map.of() : allFields(official);

            Map<String, String[]> stored = storedTranslations(tourId, langCd, koVals); // field → [번역문, 방식]
            Map<String, String> needTranslation = new LinkedHashMap<>();
            for (String field : RESPONSE_KEYS.keySet()) {
                String off = officialVals.get(field);
                String src = koVals.get(field);
                if (off != null && !AiJsonUtil.containsHangul(off)) {
                    resolved.put(field, off);
                    TourAiDetailData.FieldInfo fi = info(ORIGIN_TOURAPI); // 해당 언어 공식 값
                    fi.setTranslation("OFFICIAL");
                    meta.put(field, fi);
                } else if (src == null) {
                    // TourAPI도, 확인된 조사값도 없는 항목은 만들지 않는다.
                } else if (NO_TRANSLATE.contains(field) || !AiJsonUtil.containsHangul(src)) {
                    resolved.put(field, src);           // URL/전화번호, "09:00~18:00"처럼 번역할 게 없는 값
                    meta.get(field).setTranslation("NONE");
                } else if (stored.containsKey(field)) {
                    resolved.put(field, stored.get(field)[0]);
                    meta.get(field).setTranslation(stored.get(field)[1]);
                } else {
                    needTranslation.put(field, src);
                }
            }

            // 새 AI 번역(검증 → 저장 → 반환). 같은 관광지를 동시에 열어도 한 요청만 번역한다.
            // 화면이 AI를 오래 기다리지 않도록 최대 TRANSLATE_WAIT_MILLIS만 기다리고, 넘으면 이번엔 국문으로
            // 응답한다. 번역은 백그라운드에서 끝까지 진행돼 저장되므로 다음 조회부터 바로 나온다.
            if (!needTranslation.isEmpty() && translator != null
                    && translationService.tryClaim(tourId, "INTRO", langCd)) {
                Map<String, String> input = Map.copyOf(needTranslation);
                CompletableFuture<Map<String, String>> job = CompletableFuture
                        .supplyAsync(() -> translateAndSave(tourId, langCd, input, translator), translateExecutor)
                        .whenComplete((v, ex) -> translationService.release(tourId, "INTRO", langCd));
                try {
                    Map<String, String> saved = job.get(translateWaitMillis, TimeUnit.MILLISECONDS);
                    saved.forEach((field, t) -> {
                        resolved.put(field, t);
                        meta.get(field).setTranslation(TourTranslationService.AI);
                    });
                } catch (TimeoutException te) {
                    log.info("이용정보 번역이 늦어 이번 응답은 국문으로 보여주고 백그라운드에서 계속 번역합니다. tourId={}, lang={}", tourId, langCd);
                } catch (Exception aiEx) {
                    log.warn("이용정보 번역에 실패해 국문으로 표시합니다(번역 실패). tourId={}, lang={}", tourId, langCd, aiEx);
                }
            }
            // 번역 실패분은 국문 원문
            for (Map.Entry<String, String> e : needTranslation.entrySet()) {
                if (!resolved.containsKey(e.getKey())) {
                    resolved.put(e.getKey(), e.getValue());
                    meta.get(e.getKey()).setTranslation("KO_FALLBACK");
                }
            }
        }

        meta.keySet().retainAll(resolved.keySet());
        TourAiDetailData r = toResponse(resolved, meta);
        r.setResearchPending(pending);
        return r;
    }

    static final long TRANSLATE_WAIT_MILLIS = 2_500;
    long translateWaitMillis = TRANSLATE_WAIT_MILLIS;
    Executor translateExecutor = ForkJoinPool.commonPool();

    // AI 번역 → 검증 → 저장. 정상 번역만 돌려준다. 실패한 번역은 저장하지 않는다.
    private Map<String, String> translateAndSave(String tourId, String langCd, Map<String, String> input,
                                                 BiFunction<Map<String, String>, String, Map<String, String>> translator) {
        Map<String, String> out = new LinkedHashMap<>();
        Map<String, String> translated;
        try {
            translated = translator.apply(input, langCd);
        } catch (Exception e) {
            log.warn("이용정보 번역에 실패했습니다(번역 실패 — 저장하지 않음). tourId={}, lang={}", tourId, langCd, e);
            return out;
        }
        for (Map.Entry<String, String> e : input.entrySet()) {
            String t = translated == null ? null : translated.get(e.getKey());
            if (t == null || !TourTranslationService.isValid(e.getKey(), e.getValue(), t)) continue;
            out.put(e.getKey(), t.trim());
            try {
                translationMapper.upsertTranslation(tourId, e.getKey(), langCd, TourTranslationService.AI, e.getValue(), t.trim());
            } catch (Exception saveEx) {
                log.warn("이용정보 번역 저장에 실패했습니다(번역 자체는 성공). tourId={}, field={}", tourId, e.getKey(), saveEx);
            }
        }
        return out;
    }

    private static TourAiDetailData.FieldInfo info(String origin) {
        TourAiDetailData.FieldInfo fi = new TourAiDetailData.FieldInfo();
        fi.setOrigin(origin);
        return fi;
    }

    // ===== 캐시 + TourAPI 조회 =====

    TourIntroData load(String tourId, String langCd, String contentId) {
        TourIntroData cached = null;
        try {
            cached = translationMapper.selectIntro(tourId, langCd);
        } catch (Exception e) {
            log.warn("이용정보 캐시 조회에 실패했습니다. tourId={}, lang={}", tourId, langCd, e);
        }
        if (cached != null && isFresh(cached)) {
            return "FAIL".equals(cached.getFetchStatus()) ? null : cached;
        }

        TourIntroData fetched = fetch(tourId, langCd, contentId);
        if (fetched == null) {
            // 조회 실패: 오래된 캐시라도 있으면 그걸 쓰고, 없으면 실패를 기록해 10분간 재호출을 막는다.
            if (cached != null && !"FAIL".equals(cached.getFetchStatus())) return cached;
            TourIntroData fail = new TourIntroData();
            fail.setTourId(tourId);
            fail.setLangCd(langCd);
            fail.setContentId(contentId);
            fail.setFetchStatus("FAIL");
            saveIntro(fail);
            return null;
        }
        saveIntro(fetched);
        return fetched;
    }

    private boolean isFresh(TourIntroData cached) {
        if (cached.getFetchDt() == null) return false;
        LocalDateTime limit = "FAIL".equals(cached.getFetchStatus())
                ? LocalDateTime.now().minusMinutes(FAIL_RETRY_MINUTES)
                : LocalDateTime.now().minusDays(FRESH_DAYS);
        return cached.getFetchDt().isAfter(limit);
    }

    private void saveIntro(TourIntroData intro) {
        try {
            translationMapper.upsertIntro(intro);
        } catch (Exception e) {
            log.warn("이용정보 캐시 저장에 실패했습니다. tourId={}, lang={}", intro.getTourId(), intro.getLangCd(), e);
        }
    }

    private TourIntroData fetch(String tourId, String langCd, String contentId) {
        String commonPath = "en".equals(langCd) ? engCommonPath : "ja".equals(langCd) ? jpnCommonPath : korCommonPath;
        String introPath = commonPath.replace("detailCommon2", "detailIntro2");
        try {
            String contentTypeId = translationMapper.contentTypeId(contentId, langCd);
            JsonNode common = firstItem(call(commonPath, contentId, null));
            if (contentTypeId == null && common != null) contentTypeId = text(common, "contenttypeid");
            JsonNode intro = contentTypeId == null ? null : firstItem(call(introPath, contentId, contentTypeId));
            if (common == null && intro == null) return null;

            TourIntroData data = parse(intro, common);
            data.setTourId(tourId);
            data.setLangCd(langCd);
            data.setContentId(contentId);
            data.setContentTypeId(contentTypeId);
            data.setIntroRaw(intro == null ? null : intro.toString());
            data.setCommonRaw(common == null ? null : common.toString());
            data.setFetchStatus(isEmpty(data) ? "EMPTY" : "OK");

            // 다국어 공식 개요가 있으면 관광지 설명 번역(TOURAPI)으로 저장한다(국문 설명이 원문).
            if (!"ko".equals(langCd) && common != null) saveOfficialOverview(tourId, langCd, text(common, "overview"));
            return data;
        } catch (Exception e) {
            log.warn("TourAPI 이용정보 조회에 실패했습니다. tourId={}, lang={}, contentId={}", tourId, langCd, contentId, e);
            return null;
        }
    }

    private void saveOfficialOverview(String tourId, String langCd, String overview) {
        if (overview == null) return;
        try {
            String koOverview = translationMapper.tourContent(tourId);
            if (koOverview == null || koOverview.isBlank()) return;
            translationService.saveTourApi(tourId, TourTranslationService.TOUR_CONTENT, langCd, koOverview, overview);
        } catch (Exception e) {
            log.warn("공식 다국어 개요 저장에 실패했습니다. tourId={}, lang={}", tourId, langCd, e);
        }
    }

    private JsonNode call(String path, String contentId, String contentTypeId) {
        return restClient.get()
                .uri(b -> {
                    b.scheme("https").host(host).path(path)
                            .queryParam("serviceKey", apiKey)
                            .queryParam("MobileOS", "WEB")
                            .queryParam("MobileApp", "tripmate")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId);
                    if (contentTypeId != null) b.queryParam("contentTypeId", contentTypeId);
                    return b.build();
                })
                .retrieve()
                .body(JsonNode.class);
    }

    private static JsonNode firstItem(JsonNode response) {
        if (response == null) return null;
        JsonNode item = response.path("response").path("body").path("items").path("item");
        if (item.isArray()) return item.isEmpty() ? null : item.get(0);
        return item.isObject() ? item : null;
    }

    // ===== 응답 파싱 =====
    // 소개정보 항목명이 관광타입마다 다르다(usetime / usetimeculture / opentimefood / playtime ...).
    // 접두어로 묶어 같은 화면 항목으로 모은다. 값이 없는 항목은 null.
    static TourIntroData parse(JsonNode intro, JsonNode common) {
        TourIntroData d = new TourIntroData();
        if (intro != null) {
            Iterator<Map.Entry<String, JsonNode>> it = intro.properties().iterator();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String k = e.getKey().toLowerCase();
                String v = clean(e.getValue().asString());
                if (v == null) continue;
                if (k.equals("usetimefestival") || k.startsWith("usefee")) {
                    d.setUseFee(append(d.getUseFee(), v));
                } else if (k.startsWith("usetime") || k.startsWith("opentime") || k.equals("playtime")) {
                    d.setUseTime(append(d.getUseTime(), v));
                } else if (k.startsWith("restdate")) {
                    d.setRestDate(append(d.getRestDate(), v));
                } else if (k.startsWith("parkingfee")) {
                    d.setParkingFee(append(d.getParkingFee(), v));
                } else if (k.startsWith("parking")) {
                    d.setParking(append(d.getParking(), v));
                } else if (k.startsWith("infocenter")) {
                    d.setInfoCenter(append(d.getInfoCenter(), v));
                }
            }
        }
        if (common != null) {
            d.setTel(clean(text(common, "tel")));
            d.setHomepage(extractUrl(text(common, "homepage")));
        }
        return d;
    }

    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*[\"']?([^\"'\\s>]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL = Pattern.compile("(https?://[^\\s<\"']+|www\\.[^\\s<\"']+)", Pattern.CASE_INSENSITIVE);

    // homepage는 "<a href=\"http://...\">http://...</a>" 같은 HTML로 온다. 첫 링크만 뽑는다.
    static String extractUrl(String html) {
        if (html == null || html.isBlank()) return null;
        Matcher m = HREF.matcher(html);
        String url = m.find() ? m.group(1) : null;
        if (url == null) {
            Matcher u = URL.matcher(html);
            url = u.find() ? u.group(1) : null;
        }
        if (url == null) return null;
        url = url.replace("&amp;", "&");
        return url.startsWith("www.") ? "https://" + url : url;
    }

    // <br> 줄바꿈 유지, 나머지 태그/엔티티 제거
    static String clean(String v) {
        if (v == null) return null;
        String s = v.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n")
                .trim();
        return s.isEmpty() ? null : s;
    }

    private static String append(String prev, String v) {
        return prev == null ? v : prev.contains(v) ? prev : prev + "\n" + v;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asString();
    }

    // ===== 번역 저장소 =====

    private Map<String, String[]> storedTranslations(String tourId, String langCd, Map<String, String> koFields) {
        Map<String, String[]> out = new LinkedHashMap<>();
        if (koFields.isEmpty()) return out;
        try {
            for (Map<String, String> row : translationService.bestTranslations(tourId, langCd, "INTRO_")) {
                String field = row.get("fieldNm");
                String src = koFields.get(field);
                // 국문 원문이 바뀐 번역(해시 불일치)은 버리고 다시 번역한다.
                if (src != null && sha256(src.trim()).equalsIgnoreCase(row.get("srcHash"))) {
                    String type = row.get("transType") == null ? TourTranslationService.AI : row.get("transType");
                    out.put(field, new String[]{row.get("transText"), type});
                }
            }
        } catch (Exception e) {
            log.warn("이용정보 저장 번역 조회에 실패했습니다. tourId={}, lang={}", tourId, langCd, e);
        }
        return out;
    }

    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String safeLangContentId(String tourId, String langCd) {
        try {
            return translationMapper.langContentId(tourId, langCd);
        } catch (Exception e) {
            log.warn("다국어 콘텐츠 매핑 조회에 실패했습니다. tourId={}, lang={}", tourId, langCd, e);
            return null;
        }
    }

    private static Map<String, String> fields(TourIntroData d) {
        Map<String, String> m = new LinkedHashMap<>();
        put(m, "INTRO_USE_TIME", d.getUseTime());
        put(m, "INTRO_REST_DATE", d.getRestDate());
        put(m, "INTRO_USE_FEE", d.getUseFee());
        put(m, "INTRO_PARKING", d.getParking());
        put(m, "INTRO_PARKING_FEE", d.getParkingFee());
        put(m, "INTRO_INFO_CENTER", d.getInfoCenter());
        return m;
    }

    // 번역 대상 6개 + 홈페이지/전화
    private static Map<String, String> allFields(TourIntroData d) {
        Map<String, String> m = fields(d);
        put(m, "INTRO_TEL", d.getTel());
        put(m, "INTRO_HOMEPAGE", d.getHomepage());
        return m;
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v);
    }

    private static boolean isEmpty(TourIntroData d) {
        return fields(d).isEmpty() && blank(d.getHomepage()) && blank(d.getTel());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        return !blank(a) ? a : !blank(b) ? b : null;
    }

    // 기존 화면 응답 모양(TourAiDetailData)은 유지한다. 무료 여부/주차 가능 여부처럼 따로 주어지지
    // 않는 값은 추측하지 않고 null로 둔다. 항목별 출처/번역 상태는 fields에 담는다.
    static TourAiDetailData toResponse(Map<String, String> vals, Map<String, TourAiDetailData.FieldInfo> meta) {
        TourAiDetailData r = new TourAiDetailData();
        r.setOperatingHours(vals.get("INTRO_USE_TIME"));
        r.setClosedDays(vals.get("INTRO_REST_DATE"));
        r.setAdmissionFeeDetails(vals.get("INTRO_USE_FEE"));
        r.setParkingInfo(vals.get("INTRO_PARKING"));
        r.setParkingFeeInfo(vals.get("INTRO_PARKING_FEE"));
        r.setInfoCenter(vals.get("INTRO_INFO_CENTER"));
        r.setTel(vals.get("INTRO_TEL"));
        r.setWebsiteUrl(vals.get("INTRO_HOMEPAGE"));
        for (Map.Entry<String, TourAiDetailData.FieldInfo> e : meta.entrySet()) {
            r.getFields().put(RESPONSE_KEYS.get(e.getKey()), e.getValue());
        }
        boolean koFallback = meta.values().stream().anyMatch(f -> "KO_FALLBACK".equals(f.getTranslation()));
        boolean translated = meta.values().stream().anyMatch(f -> f.getTranslation() != null
                && !"NONE".equals(f.getTranslation()) && !"OFFICIAL".equals(f.getTranslation()));
        r.setSource(vals.isEmpty() ? SOURCE_NONE : koFallback ? SOURCE_ORIGINAL : translated ? SOURCE_TRANSLATED : SOURCE_TOURAPI);
        return r;
    }
}
