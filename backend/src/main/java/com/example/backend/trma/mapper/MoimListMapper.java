package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.MoimCateData;
import com.example.backend.trma.dto.dataList.MoimDetailData;
import com.example.backend.trma.dto.dataList.MoimPlanData;
import com.example.backend.trma.dto.dataList.MoimSearchData;
import com.example.backend.trma.dto.request.MoimSearchRequest;
import org.apache.ibatis.annotations.Mapper;

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
}
