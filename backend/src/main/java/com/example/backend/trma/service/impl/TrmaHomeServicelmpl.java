package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.BestTourListData;
import com.example.backend.trma.dto.dataList.CategoryInfoData;
import com.example.backend.trma.dto.dataList.UserInfoData;
import com.example.backend.trma.dto.dataList.BestMoimListData;
import com.example.backend.trma.dto.request.TourCategoryRequest;
import com.example.backend.trma.dto.request.UserInfoRequest;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.TrmaHomeMapper;
import com.example.backend.trma.service.TrmaHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrmaHomeServicelmpl implements TrmaHomeService {

    private final TrmaHomeMapper trmaHomeMapper;

    //사용자 정보 조회
    public UserInfoResponse userInfo(UserInfoRequest request) {

        try {
            UserInfoData UserInfo = trmaHomeMapper.userInfo(request);
            System.out.println("UserInfo: " + UserInfo);
            return new UserInfoResponse(
                    true,
                    200,
                    "SUCCESS",
                    "사용자 정보를 정상적으로 조회했습니다.",
                    "/trmaHome/userInfo",
                    "",
                    UserInfo
            );
        } catch (Exception e) {
            return new UserInfoResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/trmaHome/userInfo",
                    "",
                    null
            );
        }
    }

    //사용자 정보 조회
    public TourCategoryResponse tourCategory(TourCategoryRequest request) {

        try {
            List<CategoryInfoData> CategoryInfo = trmaHomeMapper.tourCategory(request);

            return new TourCategoryResponse(
                    true,
                    200,
                    "SUCCESS",
                    "공통코드를 정상적으로 조회했습니다.",
                    "/trmaHome/tourCategory",
                    "",
                    CategoryInfo
            );
        } catch (Exception e) {
            return new TourCategoryResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/trmaHome/tourCategory",
                    "",
                    null
            );
        }
    }

    //인기 여행지(리뷰기반)
    public BestTourListResponse bestTourList() {

        try {
            List<BestTourListData> BestTourList = trmaHomeMapper.bestTourList();

            return new BestTourListResponse(
                    true,
                    200,
                    "SUCCESS",
                    "인기 모임을 정상적으로 조회했습니다.",
                    "/trmaHome/tourCategory",
                    "",
                    BestTourList
            );
        } catch (Exception e) {
            return new BestTourListResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/trmaHome/tourCategory",
                    "",
                    null
            );
        }
    }

    //인기 모임(클릭 수 많은 모임)
    public BestMoimListResponse bestMoimList() {

        try {
            List<BestMoimListData> bestMoimList = trmaHomeMapper.bestMoimList();

            return new BestMoimListResponse(
                    true,
                    200,
                    "SUCCESS",
                    "인기 모임을 정상적으로 조회했습니다.",
                    "/trmaHome/tourCategory",
                    "",
                    bestMoimList
            );
        } catch (Exception e) {
            return new BestMoimListResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/trmaHome/tourCategory",
                    "",
                    null
            );
        }
    }
}
