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
    //모임 상세조회(기본)
    MoimDetailData moimDetail(String moimId);
    //모임 카테고리 조회
    List<MoimCateData> moimCate(String moimId);
    //모임 상세조회(일정)
    List<MoimPlanData> moimPlan(@Param("moimId") String moimId, @Param("lang") String lang);
    //모임 가입여부 검토
    MoimJoinStatusData moimJoinStatus(String moimId, String userId);
    //내 모임 목록 조회
    List<MyMoimData> myMoim(String userId);
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

    //소모임 제목/소개 번역 캐시 저장
    int updateMoimTranslation(@Param("moimId") String moimId,
                              @Param("moimTitleEn") String moimTitleEn,
                              @Param("moimTitleJa") String moimTitleJa,
                              @Param("moimDscrEn") String moimDscrEn,
                              @Param("moimDscrJa") String moimDscrJa);
}
