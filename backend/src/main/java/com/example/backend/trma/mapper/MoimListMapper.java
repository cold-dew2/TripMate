package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.CreateMoimRequest;
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
    List<MoimPlanData> moimPlan(String moimId);
    //모임 가입여부 검토
    MoimJoinStatusData moimJoinStatus(String moimId, String userId);
    //내 모임 목록 조회
    List<MyMoimData> myMoim(String userId);
    //모임 테마 조회
    List<MoimCateData> moimCateSearch();
    //모임ID 생성
    String moimIdCreate();
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
}
