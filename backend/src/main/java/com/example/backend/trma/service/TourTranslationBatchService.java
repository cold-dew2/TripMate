package com.example.backend.trma.service;

import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.util.AiErrorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 번역 누락 항목을 기존 AI 번역 기능으로 배치 단위로 채운다.
// - 대상: 최종 표시값이 비어 있고(수동/공식/기존/AI 유효 번역 없음) 원문에 한글이 있는 사용 중 관광지
// - 저장: TourTranslationService(AI 방식, 원문 해시 포함). 수동/공식 번역은 우선순위상 그대로 이긴다.
// - 같은 항목을 다른 요청이 번역 중이면 건너뛴다(in-flight 잠금).
// - Gemini 한도/크레딧 소진(429/402)이면 즉시 멈추고, 남은 항목은 그대로 둔다(다음 실행에서 재처리).
@Service
@RequiredArgsConstructor
@Slf4j
public class TourTranslationBatchService {

    static final Map<String, String> FIELD_COLUMNS = Map.of(
            "TOUR_NM", "TOUR_NM", "ROAD_ADDR", "ROAD_ADDR", "DETAIL_ADDR", "DETAIL_ADDR", "TOUR_CONTENT", "TOUR_CONTENT");

    private final TourTranslationMapper translationMapper;
    private final TourTranslationService translationService;
    private final TourListService tourListService;

    public record BatchResult(String field, String lang, int batches, int attempted, int saved, int failed,
                              int skippedInFlight, String stopReason) {}

    public BatchResult translateMissing(String field, String lang, int batchSize, int maxBatches) {
        if (!FIELD_COLUMNS.containsKey(field)) throw new IllegalArgumentException("지원하지 않는 항목: " + field);
        if (!"en".equals(lang) && !"ja".equals(lang)) throw new IllegalArgumentException("지원하지 않는 언어: " + lang);
        String column = FIELD_COLUMNS.get(field) + ("en".equals(lang) ? "_EN" : "_JA");
        int size = Math.max(1, Math.min(batchSize, 50));

        int batches = 0, attempted = 0, saved = 0, failed = 0, skipped = 0, zeroStreak = 0;
        String stop = "완료(남은 누락 없음)";
        String after = "";
        while (batches < maxBatches) {
            if (AiErrorUtil.quotaCooldownMillisRemaining() > 0) {
                stop = "Gemini 사용량 한도/크레딧 소진으로 중단";
                break;
            }
            List<Map<String, String>> rows = translationMapper.missingTranslations(field, column, size, after);
            if (rows.isEmpty()) break;
            after = rows.get(rows.size() - 1).get("tourId");
            batches++;

            // 이름은 translateTourNames 안에서 관광지별 잠금을 직접 잡으므로 여기서 잡으면 안 된다
            // (잡아 두면 안쪽에서 "다른 요청이 번역 중"으로 보고 아무것도 번역하지 않는다).
            boolean names = "TOUR_NM".equals(field);
            Map<String, String> input = new LinkedHashMap<>();
            for (Map<String, String> r : rows) {
                if (names || translationService.tryClaim(r.get("tourId"), field, lang)) input.put(r.get("tourId"), r.get("source"));
                else skipped++;
            }
            if (input.isEmpty()) continue;
            attempted += input.size();
            try {
                Map<String, String> translated = "TOUR_NM".equals(field)
                        ? tourListService.translateTourNames(new ArrayList<>(input.keySet()), lang)
                        : tourListService.translateFreeTexts(input, lang);
                List<TourTranslationService.Entry> entries = new ArrayList<>();
                input.forEach((tourId, src) -> {
                    String t = translated == null ? null : translated.get(tourId);
                    if (TourTranslationService.isValid(field, src, t)) {
                        entries.add(new TourTranslationService.Entry(tourId, field, src, t));
                    }
                });
                // 이름은 translateTourNames 안에서 이미 저장되므로 여기서는 이름 외 항목만 저장한다(중복 저장 방지).
                int ok = "TOUR_NM".equals(field) ? entries.size() : translationService.saveAiAll(lang, entries);
                saved += ok;
                failed += input.size() - ok;
                zeroStreak = ok == 0 ? zeroStreak + 1 : 0;
            } finally {
                if (!names) translationService.releaseAll(input.keySet(), field, lang);
            }
            log.info("번역 배치 {} {} #{}: 시도 {}, 저장 {}, 실패 {}", field, lang, batches, attempted, saved, failed);
            if (AiErrorUtil.quotaCooldownMillisRemaining() > 0) {
                stop = "Gemini 사용량 한도/크레딧 소진으로 중단";
                break;
            }
            if (zeroStreak >= 2) {
                stop = "연속 2개 배치 저장 0건(AI 오류 추정)으로 중단";
                break;
            }
            if (batches == maxBatches) stop = "최대 배치 수 도달(남은 항목은 다음 실행에서 처리)";
        }
        return new BatchResult(field, lang, batches, attempted, saved, failed, skipped, stop);
    }

    // ===== 외부 번역 엔진(Claude 등) 결과 가져오기 =====
    // Gemini가 아닌 엔진으로 번역한 결과를 같은 저장 규칙으로 넣는다. 번역 방식은 AI로 저장하고
    // (수동/공식/기존 번역 행은 방식별 PK라 건드리지 않음), 다음 경우는 저장하지 않는다.
    //  - 내보낸 뒤 원문이 바뀜(changed)  - 그 사이 다른 유효 번역이 생김(already)
    //  - 실패 번역(빈 값/원문 그대로/이름·주소에 한글 잔존/키릴 문자)(invalid)

    public record ImportItem(String tourId, String field, String lang, String source, String text) {}

    public record ImportResult(int received, int saved, int invalid, int changed, int already, int skippedInFlight, int failed,
                               List<String> problems) {}

    private static final java.util.regex.Pattern CYRILLIC = java.util.regex.Pattern.compile("[\\u0400-\\u04FF]");

    public List<Map<String, String>> exportMissing(String field, String lang, int limit, String afterTourId) {
        String column = column(field, lang);
        return translationMapper.missingTranslations(field, column, Math.max(1, Math.min(limit, 1000)),
                afterTourId == null ? "" : afterTourId);
    }

    public ImportResult importTranslations(List<ImportItem> items) {
        int saved = 0, invalid = 0, changed = 0, already = 0, skipped = 0, failed = 0;
        List<String> problems = new ArrayList<>();
        java.util.LinkedHashSet<String> touched = new java.util.LinkedHashSet<>();
        for (ImportItem it : items) {
            String column;
            try {
                column = column(it.field(), it.lang());
            } catch (IllegalArgumentException e) {
                invalid++; problems.add(it.tourId() + " " + e.getMessage()); continue;
            }
            String text = it.text() == null ? null : it.text().trim();
            if (it.source() == null || !TourTranslationService.isValid(it.field(), it.source(), text)
                    || CYRILLIC.matcher(text).find()) {
                invalid++; problems.add(it.tourId() + " " + it.field() + " " + it.lang() + " invalid"); continue;
            }
            if (!translationService.tryClaim(it.tourId(), it.field(), it.lang())) { skipped++; continue; }
            try {
                Map<String, String> cur = translationMapper.currentState(it.field(), column, it.tourId());
                if (cur == null || cur.get("source") == null || !cur.get("source").equals(it.source().trim())) { changed++; continue; }
                if (cur.get("value") != null) { already++; continue; }
                translationMapper.upsertTranslation(it.tourId(), it.field(), it.lang(), TourTranslationService.AI, it.source(), text);
                touched.add(it.tourId());
                saved++;
            } catch (Exception e) {
                failed++; problems.add(it.tourId() + " " + it.field() + " " + it.lang() + " save failed: " + e.getMessage());
                log.warn("번역 가져오기 저장 실패 tourId={}, field={}, lang={}", it.tourId(), it.field(), it.lang(), e);
            } finally {
                translationService.release(it.tourId(), it.field(), it.lang());
            }
        }
        if (!touched.isEmpty()) translationService.resolve(new ArrayList<>(touched));
        return new ImportResult(items.size(), saved, invalid, changed, already, skipped, failed,
                problems.size() > 50 ? problems.subList(0, 50) : problems);
    }

    // ===== AI 번역 교체(공식 명칭 맞춤 등) =====
    // 최종 표시값이 AI 번역이고, 그 값이 요청의 oldText와 같을 때만 AI 행을 새 번역으로 바꾼다.
    // 수동/공식/기존 번역이 표시 중이면(notAi) 또는 그 사이 값이 바뀌었으면(changed) 건드리지 않는다.
    public record ReplaceItem(String tourId, String field, String lang, String source, String oldText, String text) {}

    public record ReplaceResult(int received, int replaced, int invalid, int changed, int notAi, int skippedInFlight, int failed,
                                List<String> problems) {}

    public ReplaceResult replaceAiTranslations(List<ReplaceItem> items) {
        int replaced = 0, invalid = 0, changed = 0, notAi = 0, skipped = 0, failed = 0;
        List<String> problems = new ArrayList<>();
        java.util.LinkedHashSet<String> touched = new java.util.LinkedHashSet<>();
        for (ReplaceItem it : items) {
            String column;
            try {
                column = column(it.field(), it.lang());
            } catch (IllegalArgumentException e) {
                invalid++; problems.add(it.tourId() + " " + e.getMessage()); continue;
            }
            String text = it.text() == null ? null : it.text().trim();
            if (it.source() == null || it.oldText() == null || !TourTranslationService.isValid(it.field(), it.source(), text)
                    || CYRILLIC.matcher(text).find()) {
                invalid++; problems.add(it.tourId() + " " + it.field() + " " + it.lang() + " invalid"); continue;
            }
            if (!translationService.tryClaim(it.tourId(), it.field(), it.lang())) { skipped++; continue; }
            try {
                Map<String, String> cur = translationMapper.currentState(it.field(), column, it.tourId());
                if (cur == null || cur.get("source") == null || !cur.get("source").equals(it.source().trim())) { changed++; continue; }
                Map<String, String> best = translationMapper.bestTranslation(it.field(), it.lang(), it.tourId());
                if (best == null || !TourTranslationService.AI.equals(best.get("transType"))) { notAi++; continue; }
                if (!it.oldText().trim().equals(best.get("transText") == null ? null : best.get("transText").trim())) { changed++; continue; }
                translationMapper.upsertTranslation(it.tourId(), it.field(), it.lang(), TourTranslationService.AI, it.source(), text);
                touched.add(it.tourId());
                replaced++;
            } catch (Exception e) {
                failed++; problems.add(it.tourId() + " " + it.field() + " " + it.lang() + " replace failed: " + e.getMessage());
                log.warn("AI 번역 교체 실패 tourId={}, field={}, lang={}", it.tourId(), it.field(), it.lang(), e);
            } finally {
                translationService.release(it.tourId(), it.field(), it.lang());
            }
        }
        if (!touched.isEmpty()) translationService.resolve(new ArrayList<>(touched));
        return new ReplaceResult(items.size(), replaced, invalid, changed, notAi, skipped, failed,
                problems.size() > 50 ? problems.subList(0, 50) : problems);
    }

    private static String column(String field, String lang) {
        if (!FIELD_COLUMNS.containsKey(field)) throw new IllegalArgumentException("지원하지 않는 항목: " + field);
        if (!"en".equals(lang) && !"ja".equals(lang)) throw new IllegalArgumentException("지원하지 않는 언어: " + lang);
        return FIELD_COLUMNS.get(field) + ("en".equals(lang) ? "_EN" : "_JA");
    }

    // 번역 현황 리포트(항목·언어별 상태, 수동 번역 덮어쓰기/중복 행 검증 포함)
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String field : List.of("TOUR_NM", "ROAD_ADDR", "DETAIL_ADDR", "TOUR_CONTENT")) {
            for (String lang : List.of("en", "ja")) {
                String column = column(field, lang);
                Map<String, Object> row = new LinkedHashMap<>(translationMapper.translationStatus(field, column, lang));
                row.put("manualOverridden", translationMapper.manualOverridden(field, column, lang));
                out.put(field + "_" + lang, row);
            }
        }
        out.put("duplicateRows", translationMapper.duplicateTranslationRows());
        out.put("typeCounts", translationMapper.translationTypeCounts());
        return out;
    }
}
