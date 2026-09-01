package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.BestMoimListData;
import com.example.backend.trma.dto.dataList.BestTourListData;
import com.example.backend.trma.dto.dataList.CategoryInfoData;
import com.example.backend.trma.dto.dataList.UserInfoData;
import com.example.backend.trma.dto.request.TourCategoryRequest;
import com.example.backend.trma.dto.request.UserInfoRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TrmaHomeMapper {
    //사용자 정보 조회
    UserInfoData userInfo(String request);
    //공통코드(카테고리)
    List<CategoryInfoData> tourCategory(TourCategoryRequest request);
    //인기 여행지(리뷰기반)
    List<BestTourListData> bestTourList();
    //인기 모임(클릭 수 많은 모임)
    List<BestMoimListData> bestMoimList();
}
