package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.CreateMoimRequest;
import com.example.backend.trma.dto.request.CreateMoimReviewRequest;
import com.example.backend.trma.dto.request.MoimSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MoimListMapper {
    //모임 목록 조회
    List<MoimSearchData> moimSearch(MoimSearchRequest request);
    //모임 정보
    MoimSearchData moimInfo(String moimId);
    //모임장(생성자) 계정 조회
    String moimHostUserId(String moimId);
    //소모임 제목 일괄 조회(번역 캐시 확인용)
    List<MoimTitleTranslationData> moimTitlesByIds(@Param("moimIds") List<String> moimIds);
    //모임 상세조회(기본)
    MoimDetailData moimDetail(String moimId);
    //모임 카테고리 조회
    List<MoimCateData> moimCate(String moimId);
    //모임 상세조회(일정)
    List<MoimPlanData> moimPlan(@Param("moimId") String moimId, @Param("lang") String lang);
    //모임 가입여부 검토
    MoimJoinStatusData moimJoinStatus(String moimId, String userId);
    //모임 후기(관광지/소모임) 작성 여부 조회
    MoimReviewStatusData moimReviewStatus(String moimId, String userId);

    //모임 상세조회 방문 이력 등록(사용자별 모임별 최초 1회만)
    int insertMoimVisitIfNotExists(@Param("moimId") String moimId, @Param("userId") String userId);
    //내 모임 목록 조회
    List<MyMoimData> myMoim(String userId);
    //내가 가입한 모임 중 오늘 진행 중인 모임들의 오늘 일정
    List<MyTodayScheduleRowData> myTodaySchedule(@Param("userId") String userId, @Param("today") String today);
    //모임 테마 조회
    List<MoimCateData> moimCateSearch();
    //모임 등록
    int createMoimList(@Param("request") CreateMoimRequest request,
                       @Param("moimId") String moimId,
                       @Param("userId") String userId);
    //모임 테마 등록
    int insertMoimCate(@Param("cateData") MoimCateData cateData,
                       @Param("moimId") String moimId,
                       @Param("userId") String userId);

    //모임 일정 등록
    int insertMoimPlan(@Param("moimPlan") MoimPlanInsertData moimPlan,
                       @Param("moimId") String moimId,
                       @Param("userId") String userId);
    //모임 대표 이미지를 첫 일정 관광지 이미지로 갱신
    int updateMoimImgFromFirstPlan(@Param("moimId") String moimId);

    //모임 일정 삭제(전체 교체용)
    int deleteMoimPlan(@Param("moimId") String moimId);

    //모임 종료일 수정
    int updateMoimEndDt(@Param("moimId") String moimId,
                        @Param("moimEndDt") String moimEndDt,
                        @Param("userId") String userId);

    //모임 멤버 등록
    int insertMoimMember(@Param("moimId") String moimId,
                         @Param("userId") String userId,
                         @Param("roleCd") String roleCd,
                         @Param("stateCd") String stateCd);

    //모임 멤버 목록 조회
    List<MoimMemberData> moimMembers(String moimId);

    //모임 멤버 상태 변경(승인)
    int updateMoimMemberState(@Param("moimId") String moimId,
                              @Param("targetUserId") String targetUserId,
                              @Param("stateCd") String stateCd,
                              @Param("userId") String userId);

    //모임 멤버 삭제(거절)
    int deleteMoimMember(@Param("moimId") String moimId,
                         @Param("targetUserId") String targetUserId);

    //모임(여행) 후기 등록
    int insertMoimReview(@Param("reviewId") String reviewId,
                         @Param("moimId") String moimId,
                         @Param("request") CreateMoimReviewRequest request,
                         @Param("imgUrls") String imgUrls,
                         @Param("userId") String userId);

    //모임(여행) 후기 목록 조회
    List<MoimReviewData> moimReviews(String moimId);

    //모임 후기 번역 캐시 저장
    int updateReviewTranslation(@Param("reviewId") String reviewId,
                                @Param("reviewContentEn") String reviewContentEn,
                                @Param("reviewContentJa") String reviewContentJa);

    //소모임 제목/소개 번역 캐시 저장
    int updateMoimTranslation(@Param("moimId") String moimId,
                              @Param("moimTitleEn") String moimTitleEn,
                              @Param("moimTitleJa") String moimTitleJa,
                              @Param("moimDscrEn") String moimDscrEn,
                              @Param("moimDscrJa") String moimDscrJa);
}
