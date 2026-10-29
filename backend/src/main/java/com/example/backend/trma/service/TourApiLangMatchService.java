package com.example.backend.trma.service;

import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.util.AiJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 한국관광공사 TourAPI 다국어 서비스(EngService2/JpnService2) 콘텐츠를 국문 관광지에 연결하고,
// 공식 번역(이름/주소/설명)을 번역 저장소에 TOURAPI 방식으로 저장한다.
//
// 다국어 서비스는 국문(KorService2)과 콘텐츠ID 체계가 달라 ID로는 연결되지 않는다(예전 조인이
// 항상 0건이던 원인). 대신 다국어 제목에는 거의 항상 국문명이 괄호로 붙어 있으므로
// ("Biyangdo Island (비양도)", "済州ラブランド（제주러브랜드）"),
//   1) 좌표가 약 1km 이내(위경도 차 0.01도 미만)이고, 그 범위의 같은 이름 후보가 하나뿐이고
//   2) 괄호 속 국문명이 국문 관광지명과 (공백/괄호 제거 후) 정확히 같을 때만 연결한다.
// 좌표만 보면 같은 건물의 다른 시설(코엑스 아쿠아리움 ↔ 코엑스아티움)이 잘못 붙는다.
@Service
@RequiredArgsConstructor
@Slf4j
public class TourApiLangMatchService {

    public static final String MATCH_TYPE = "COORD_NAME";

    private final TourTranslationMapper translationMapper;

    public record MatchResult(String lang, int candidates, int matched, int translationsSaved) {}

    public List<MatchResult> matchAll() {
        List<MatchResult> results = new ArrayList<>();
        for (String lang : List.of("en", "ja")) {
            results.add(match(lang));
        }
        // 공식 번역이 들어왔으니 최종 표시값을 전체 다시 계산한다(수동 번역은 그대로 우선).
        translationMapper.resolveTourColumns(List.of());
        return results;
    }

    // 좌표 허용 오차(도). 위경도 0.01도 ≈ 1km. 예전에는 소수점 셋째 자리로 반올림한 값이 같아야
    // 했는데, 반올림 경계에 걸리면 99m 떨어진 같은 장소도 놓쳤다(서귀포매일올레시장 등).
    static final double MAX_COORD_DIFF = 0.01;

    MatchResult match(String lang) {
        List<Map<String, Object>> contents = translationMapper.langContentsForMatch(lang);
        // 괄호 속 국문명(정규화) → 후보 목록
        Map<String, List<Map<String, Object>>> byName = new HashMap<>();
        for (Map<String, Object> c : contents) {
            String koName = extractKoreanName(str(c.get("title")));
            if (koName != null && coord(c.get("mapX")) != null && coord(c.get("mapY")) != null) {
                byName.computeIfAbsent(normalize(koName), x -> new ArrayList<>()).add(c);
            }
        }

        int matched = 0, saved = 0;
        for (Map<String, Object> tour : translationMapper.koreanToursForMatch()) {
            String tourNm = str(tour.get("tourNm"));
            Double x = coord(tour.get("mapX")), y = coord(tour.get("mapY"));
            List<Map<String, Object>> sameName = byName.get(normalize(tourNm));
            if (sameName == null || x == null || y == null) continue;

            // 이름이 정확히 같고 약 1km 안에 있는 후보가 딱 하나일 때만 연결한다(체인점 등 모호하면 건너뜀).
            List<Map<String, Object>> near = sameName.stream()
                    .filter(c -> Math.abs(coord(c.get("mapX")) - x) < MAX_COORD_DIFF
                            && Math.abs(coord(c.get("mapY")) - y) < MAX_COORD_DIFF)
                    .toList();
            if (near.size() != 1) continue;
            Map<String, Object> hit = near.get(0);

            String tourId = str(tour.get("tourId"));
            String contentId = str(hit.get("contentId"));
            try {
                translationMapper.upsertLangMap(tourId, lang, contentId, MATCH_TYPE);
                matched++;
                saved += saveOfficial(tourId, lang, TourTranslationService.TOUR_NM, tourNm, stripKoreanName(str(hit.get("title"))));
                saved += saveOfficial(tourId, lang, TourTranslationService.ROAD_ADDR, str(tour.get("roadAddr")), str(hit.get("addr1")));
                saved += saveOfficial(tourId, lang, TourTranslationService.DETAIL_ADDR, str(tour.get("detailAddr")), str(hit.get("addr2")));
            } catch (Exception e) {
                log.warn("다국어 콘텐츠 매칭 저장에 실패했습니다. tourId={}, lang={}, contentId={}", tourId, lang, contentId, e);
            }
        }
        log.info("TourAPI 다국어 매칭 완료: lang={}, 후보 {}건, 연결 {}건, 공식 번역 저장 {}건", lang, contents.size(), matched, saved);
        return new MatchResult(lang, contents.size(), matched, saved);
    }

    private int saveOfficial(String tourId, String lang, String field, String source, String official) {
        if (source == null || source.isBlank() || official == null || official.isBlank()) return 0;
        if (!TourTranslationService.isValid(field, source, official)) return 0;
        translationMapper.upsertTranslation(tourId, field, lang, TourTranslationService.TOURAPI, source, official.trim());
        return 1;
    }

    static Double coord(Object v) {
        try {
            return v == null || v.toString().isBlank() ? null : Double.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            return null; // "null" 같은 문자열
        }
    }

    // 제목 끝의 (중첩 가능한) 괄호 묶음 중 한글이 든 것을 국문명으로 본다.
    // "산방산（済州）（산방산（제주））" → "산방산（제주）"
    static String extractKoreanName(String title) {
        if (title == null) return null;
        String t = title.trim();
        int end = t.length() - 1;
        while (end >= 0) {
            char close = t.charAt(end);
            if (close != ')' && close != '）') return null;
            int depth = 0;
            int start = -1;
            for (int i = end; i >= 0; i--) {
                char ch = t.charAt(i);
                if (ch == ')' || ch == '）') depth++;
                else if (ch == '(' || ch == '（') {
                    depth--;
                    if (depth == 0) { start = i; break; }
                }
            }
            if (start < 0) return null;
            String inner = t.substring(start + 1, end).trim();
            if (AiJsonUtil.containsHangul(inner)) return inner;
            end = start - 1;
            while (end >= 0 && Character.isWhitespace(t.charAt(end))) end--;
        }
        return null;
    }

    // 다국어 제목에서 국문명 괄호를 뗀 공식 표기
    static String stripKoreanName(String title) {
        String ko = extractKoreanName(title);
        if (ko == null) return title == null ? null : title.trim();
        String t = title.trim();
        int idx = t.lastIndexOf(ko);
        String head = idx > 0 ? t.substring(0, idx - 1) : t;
        return head.trim();
    }

    static String normalize(String s) {
        return s == null ? "" : s.replaceAll("[\\s()（）\\[\\]·・,.]", "");
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
