package com.example.backend.trma.service;

import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.util.AiJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 관광지 다국어 번역 저장소.
//
// 표시 우선순위: MANUAL(수동 검수) > TOURAPI(공식 다국어 데이터) > LEGACY(출처 미확인 기존값) > AI(Gemini)
// 번역은 TB_TRMA_TOUR_TRANS에 방식별로 저장하고, 원문 해시가 현재 원문과 같은 번역 중
// 우선순위가 가장 높은 값을 TB_TRMA_TOUR_LIST의 *_EN/*_JA 컬럼(최종 표시값)에 반영한다.
// 그래서 목록/상세 조회 쿼리는 기존처럼 컬럼만 읽으면 되고(추가 조인·N+1 없음),
// 원문이 바뀐 관광지는 재계산 시 오래된 번역이 자동으로 빠진다.
@Service
@RequiredArgsConstructor
@Slf4j
public class TourTranslationService {

    public static final String MANUAL = "MANUAL";
    public static final String TOURAPI = "TOURAPI";
    public static final String AI = "AI";

    public static final String TOUR_NM = "TOUR_NM";
    public static final String TOUR_CONTENT = "TOUR_CONTENT";
    public static final String ROAD_ADDR = "ROAD_ADDR";
    public static final String DETAIL_ADDR = "DETAIL_ADDR";

    private final TourTranslationMapper translationMapper;

    // 같은 (관광지, 필드, 언어)에 대해 동시에 AI 번역이 여러 번 돌지 않도록, 진행 중인 키를 잡아 둔다.
    // 인스턴스 하나짜리 배포라 메모리로 충분하고, 최종 중복 저장은 DB 기본키가 막는다.
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    private static String key(String tourId, String field, String lang) {
        return tourId + "|" + field + "|" + lang;
    }

    // AI 번역을 시작해도 되는지(이미 다른 요청이 같은 항목을 번역 중이면 false).
    public boolean tryClaim(String tourId, String field, String lang) {
        return inFlight.add(key(tourId, field, lang));
    }

    public void release(String tourId, String field, String lang) {
        inFlight.remove(key(tourId, field, lang));
    }

    public void releaseAll(Collection<String> tourIds, String field, String lang) {
        for (String tourId : tourIds) release(tourId, field, lang);
    }

    // 번역 결과가 저장해도 되는 정상 번역인지. 비었거나 원문을 그대로 돌려준 경우(한글이 남은
    // 이름/주소), 원문과 똑같은 경우는 실패로 보고 저장하지 않는다(다음 조회 때 다시 시도).
    public static boolean isValid(String field, String source, String translated) {
        if (translated == null || translated.isBlank()) return false;
        if (source != null && translated.trim().equals(source.trim())) return false;
        if (!TOUR_CONTENT.equals(field) && AiJsonUtil.containsHangul(translated)) return false;
        return true;
    }

    // AI 번역 저장 후 최종 표시값을 다시 계산한다. 저장 실패는 AI 호출 실패와 구분해 로그만 남기고
    // false를 돌려준다(호출부는 이번 응답에는 번역값을 그대로 보여줘도 된다).
    public boolean saveAi(String tourId, String field, String lang, String source, String translated) {
        return save(tourId, field, lang, AI, source, translated);
    }

    public record Entry(String tourId, String field, String source, String translated) {}

    // 여러 AI 번역을 한 번에 저장하고 최종 표시값 재계산은 관광지 묶음으로 한 번만 한다(목록 번역 등).
    // 정상 번역이 아닌 항목은 건너뛰고, 실제 저장된 건수를 돌려준다.
    public int saveAiAll(String lang, List<Entry> entries) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return 0;
        if (entries == null || entries.isEmpty()) return 0;
        int saved = 0;
        LinkedHashSet<String> tourIds = new LinkedHashSet<>();
        for (Entry e : entries) {
            if (e.source() == null || e.source().isBlank() || !isValid(e.field(), e.source(), e.translated())) continue;
            try {
                translationMapper.upsertTranslation(e.tourId(), e.field(), lang, AI, e.source(), e.translated().trim());
                tourIds.add(e.tourId());
                saved++;
            } catch (Exception ex) {
                log.warn("번역 저장에 실패했습니다(번역 자체는 성공). tourId={}, field={}, lang={}",
                        e.tourId(), e.field(), lang, ex);
            }
        }
        if (!tourIds.isEmpty()) {
            try {
                translationMapper.resolveTourColumns(new ArrayList<>(tourIds));
            } catch (Exception ex) {
                log.warn("번역 최종 표시값 반영에 실패했습니다. tourIds={}", tourIds, ex);
            }
        }
        return saved;
    }

    public boolean saveTourApi(String tourId, String field, String lang, String source, String translated) {
        return save(tourId, field, lang, TOURAPI, source, translated);
    }

    public boolean saveManual(String tourId, String field, String lang, String source, String translated) {
        return save(tourId, field, lang, MANUAL, source, translated);
    }

    private boolean save(String tourId, String field, String lang, String type, String source, String translated) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return false;
        if (source == null || source.isBlank() || !isValid(field, source, translated)) return false;
        try {
            translationMapper.upsertTranslation(tourId, field, lang, type, source, translated.trim());
            translationMapper.resolveTourColumns(List.of(tourId));
            return true;
        } catch (Exception e) {
            log.warn("번역 저장에 실패했습니다(번역 자체는 성공). tourId={}, field={}, lang={}, type={}",
                    tourId, field, lang, type, e);
            return false;
        }
    }

    // 원문이 바뀐 관광지(TourAPI 동기화 후 등)의 최종 표시값을 다시 계산한다. 비우면 전체.
    public int resolve(List<String> tourIds) {
        return translationMapper.resolveTourColumns(tourIds == null ? List.of() : tourIds);
    }

    // TB_TRMA_TOUR_LIST에 컬럼이 없는 필드(예: 소개정보 INTRO_*)의 저장 번역. 원문 해시가
    // 다른(원문이 바뀐) 번역은 호출부에서 sourceHashes로 걸러 쓴다.
    public List<Map<String, String>> bestTranslations(String tourId, String lang, String fieldPrefix) {
        return translationMapper.bestTranslations(tourId, lang, fieldPrefix);
    }
}
