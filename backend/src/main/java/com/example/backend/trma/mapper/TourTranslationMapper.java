package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.TourIntroData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

// 관광지 다국어 번역 저장소(TB_TRMA_TOUR_TRANS)와 TourAPI 소개정보 캐시(TB_TRMA_TOUR_INTRO).
@Mapper
public interface TourTranslationMapper {

    // 번역 방식별 저장. 같은 (관광지, 필드, 언어, 방식)은 한 행만 유지하고 원문이 바뀌면 덮어쓴다.
    int upsertTranslation(@Param("tourId") String tourId,
                          @Param("fieldNm") String fieldNm,
                          @Param("langCd") String langCd,
                          @Param("transType") String transType,
                          @Param("srcText") String srcText,
                          @Param("transText") String transText);

    // TB_TRMA_TOUR_LIST의 *_EN/*_JA(최종 표시값)를 우선순위대로 다시 계산한다.
    // tourIds가 비어 있으면 전체를 다시 계산한다(배치/원문 변경 후).
    int resolveTourColumns(@Param("tourIds") List<String> tourIds);

    // 원문이 바뀌지 않은(해시 일치) 저장 번역 중 우선순위가 가장 높은 값. 소개정보처럼
    // TB_TRMA_TOUR_LIST에 컬럼이 없는 필드에 쓴다. key: FIELD_NM
    List<Map<String, String>> bestTranslations(@Param("tourId") String tourId,
                                               @Param("langCd") String langCd,
                                               @Param("fieldPrefix") String fieldPrefix);

    // 국문 관광지에 대응하는 다국어 서비스 콘텐츠ID (없으면 null)
    String langContentId(@Param("tourId") String tourId, @Param("langCd") String langCd);

    // detailIntro2 조회에 필요한 콘텐츠 타입 (국문/다국어 원천 목록 기준)
    String contentTypeId(@Param("contentId") String contentId, @Param("langCd") String langCd);

    TourIntroData selectIntro(@Param("tourId") String tourId, @Param("langCd") String langCd);

    int upsertIntro(TourIntroData intro);

    // ===== TourAPI 다국어 콘텐츠 매칭 =====
    // 매칭 대상 국문 관광지 (좌표가 있는 사용 중 관광지)
    List<Map<String, Object>> koreanToursForMatch();

    // 다국어 서비스 원천 목록 (en: EngService2, ja: JpnService2 지역기반목록)
    List<Map<String, Object>> langContentsForMatch(@Param("langCd") String langCd);

    int upsertLangMap(@Param("tourId") String tourId, @Param("langCd") String langCd,
                      @Param("langContentId") String langContentId, @Param("matchType") String matchType);

    // 다국어 서비스 상세(detailCommon2) 개요. 매칭된 콘텐츠의 공식 설명 번역에 쓴다.
    String langOverview(@Param("langContentId") String langContentId, @Param("langCd") String langCd);

    // 국문 관광지 설명 원문 (공식 다국어 개요 저장 시 원문 해시용)
    String tourContent(@Param("tourId") String tourId);

    // ===== AI 정보 조사 결과 (TB_TRMA_TOUR_INFO_AI) =====
    List<com.example.backend.trma.dto.dataList.TourInfoAiData> selectInfoAi(@Param("tourId") String tourId);

    int upsertInfoAi(com.example.backend.trma.dto.dataList.TourInfoAiData info);

    // 정보 조사 프롬프트용 관광지 기본 정보 (tourNm, roadAddr)
    Map<String, Object> tourBasic(@Param("tourId") String tourId);

    // 번역 누락 항목(최종 표시값이 비어 있고 원문에 한글이 있는 사용 중 관광지). field/column은 서비스에서 화이트리스트로 넘긴다.
    // key: tourId, source
    List<Map<String, String>> missingTranslations(@Param("field") String field, @Param("column") String column,
                                                  @Param("limit") int limit, @Param("afterTourId") String afterTourId);

    // 번역 가져오기(import) 시 원문 변경/기존 유효 번역 확인용. key: source(공백 정리된 현재 원문), value(현재 최종 표시값)
    Map<String, String> currentState(@Param("field") String field, @Param("column") String column, @Param("tourId") String tourId);

    // AI 번역 교체 시 확인용: 현재 원문 해시와 맞는 번역 중 최종 표시 우선순위 1위 행. key: transType, transText
    Map<String, String> bestTranslation(@Param("field") String field, @Param("lang") String lang, @Param("tourId") String tourId);

    // 번역 현황 집계(항목·언어별). total, manual, tourapi, legacy, ai, noHangul, stale, missing, columnMismatch
    Map<String, Object> translationStatus(@Param("field") String field, @Param("column") String column, @Param("lang") String lang);

    // 같은 (관광지, 항목, 언어, 방식) 중복 행 수 — PK라 0이어야 한다
    int duplicateTranslationRows();

    // 원문 해시가 유효한 수동 번역인데 최종 표시값이 그 수동 번역과 다른 건수(수동 번역 덮어쓰기) — 0이어야 한다
    int manualOverridden(@Param("field") String field, @Param("column") String column, @Param("lang") String lang);

    // 방식별 저장 행 수
    List<Map<String, Object>> translationTypeCounts();
}
