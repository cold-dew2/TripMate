package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.TourSearchRequest;
import com.example.backend.trma.dto.response.TourAPIResponse;
import com.example.backend.trma.dto.response.TourSearchResponse;
import com.example.backend.trma.service.TourAPIService;
import com.example.backend.trma.service.TourApiLangMatchService;
import com.example.backend.trma.service.TourTranslationBatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tourAPI")
public class TourAPIController {

    private final TourAPIService tourAPIService;
    private final TourApiLangMatchService langMatchService;
    private final TourTranslationBatchService translationBatchService;

    public TourAPIController(TourAPIService tourAPIService, TourApiLangMatchService langMatchService,
                             TourTranslationBatchService translationBatchService) {
        this.tourAPIService = tourAPIService;
        this.langMatchService = langMatchService;
        this.translationBatchService = translationBatchService;
    }

//    //한국관광공사_기초지자체 중심 관광지 정보
//    @GetMapping("/areaBasedList")
//    public TourAPIResponse areaBasedList() {
//
//        return tourAPIService.areaBasedList();
//    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/areaBased_batch")
    public TourAPIResponse areaBased_batch() {

        return tourAPIService.areaBased_batch();
    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/detailCommon_batch")
    public TourAPIResponse detailCommon_batch() {

        return tourAPIService.detailCommon_batch();
    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/searchFestival_batch")
    public TourAPIResponse searchFestival_batch() {

        return tourAPIService.searchFestival_batch();
    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/tourMaster_batch")
    public TourAPIResponse tourMaster_batch() {

        return tourAPIService.tourMaster_batch();
    }

    //영문/일문 관광정보 서비스 콘텐츠를 국문 관광지에 연결하고 공식 번역을 반영
    @GetMapping("/langMatch_batch")
    public TourAPIResponse langMatch_batch() {
        try {
            return new TourAPIResponse(true, 200, "SUCCESS",
                    "다국어 매칭 완료: " + langMatchService.matchAll(), "/tourAPI/langMatch_batch", null);
        } catch (Exception e) {
            return new TourAPIResponse(false, 500, "FAIL",
                    "다국어 매칭 오류: " + e.getMessage(), "/tourAPI/langMatch_batch", null);
        }
    }

    //번역 누락 항목을 AI 번역으로 배치 처리. AI 사용량이 드는 작업이라 서버 로컬에서만 호출할 수 있다.
    @GetMapping("/translateMissing_batch")
    public TourAPIResponse translateMissing_batch(HttpServletRequest request,
                                                  @RequestParam String field, @RequestParam String lang,
                                                  @RequestParam(defaultValue = "20") int batchSize,
                                                  @RequestParam(defaultValue = "5") int maxBatches) {
        if (!isLocal(request)) {
            return new TourAPIResponse(false, 403, "FORBIDDEN", "로컬에서만 실행할 수 있습니다.", "/tourAPI/translateMissing_batch", null);
        }
        try {
            return new TourAPIResponse(true, 200, "SUCCESS",
                    String.valueOf(translationBatchService.translateMissing(field, lang, batchSize, maxBatches)),
                    "/tourAPI/translateMissing_batch", null);
        } catch (Exception e) {
            return new TourAPIResponse(false, 500, "FAIL", "번역 배치 오류: " + e.getMessage(), "/tourAPI/translateMissing_batch", null);
        }
    }

    private static boolean isLocal(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return "127.0.0.1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote);
    }

    //번역 누락 항목 목록(외부 번역 엔진에 넘길 원문). 서버 로컬에서만.
    @GetMapping("/missingTranslations")
    public ResponseEntity<?> missingTranslations(HttpServletRequest request,
                                                 @RequestParam String field, @RequestParam String lang,
                                                 @RequestParam(defaultValue = "100") int limit,
                                                 @RequestParam(defaultValue = "") String after) {
        if (!isLocal(request)) return ResponseEntity.status(403).body(Map.of("message", "로컬에서만 실행할 수 있습니다."));
        return ResponseEntity.ok(translationBatchService.exportMissing(field, lang, limit, after));
    }

    //외부 번역 엔진(Claude 등) 번역 결과를 번역 저장소에 AI 방식으로 저장. 서버 로컬에서만.
    @PostMapping("/importTranslations")
    public ResponseEntity<?> importTranslations(HttpServletRequest request,
                                                @RequestBody List<TourTranslationBatchService.ImportItem> items) {
        if (!isLocal(request)) return ResponseEntity.status(403).body(Map.of("message", "로컬에서만 실행할 수 있습니다."));
        return ResponseEntity.ok(translationBatchService.importTranslations(items));
    }

    //AI 번역 교체(공식 명칭 맞춤 등). 표시 중인 값이 AI 번역일 때만 바꾼다. 서버 로컬에서만.
    @PostMapping("/replaceAiTranslations")
    public ResponseEntity<?> replaceAiTranslations(HttpServletRequest request,
                                                   @RequestBody List<TourTranslationBatchService.ReplaceItem> items) {
        if (!isLocal(request)) return ResponseEntity.status(403).body(Map.of("message", "로컬에서만 실행할 수 있습니다."));
        return ResponseEntity.ok(translationBatchService.replaceAiTranslations(items));
    }

    //번역 현황(항목·언어별 상태, 수동 번역 덮어쓰기/중복 검증). 서버 로컬에서만.
    @GetMapping("/translationStatus")
    public ResponseEntity<?> translationStatus(HttpServletRequest request) {
        if (!isLocal(request)) return ResponseEntity.status(403).body(Map.of("message", "로컬에서만 실행할 수 있습니다."));
        return ResponseEntity.ok(translationBatchService.status());
    }
}
