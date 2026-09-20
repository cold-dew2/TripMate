package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.BestTourListData;
import com.example.backend.trma.dto.dataList.CategoryInfoData;
import com.example.backend.trma.dto.dataList.UserInfoData;
import com.example.backend.trma.dto.dataList.BestMoimListData;
import com.example.backend.trma.dto.request.TourCategoryRequest;
import com.example.backend.trma.dto.request.UserInfoRequest;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.TrmaHomeMapper;
import com.example.backend.trma.service.MoimListService;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.service.TrmaHomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrmaHomeServicelmpl implements TrmaHomeService {

    private final TrmaHomeMapper trmaHomeMapper;
    private final TourListService tourListService;
    private final MoimListService moimListService;

    //사용자 정보 조회
    public UserInfoResponse userInfo(String request) {

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
            log.error("처리 중 오류가 발생했습니다.", e);
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
            log.error("처리 중 오류가 발생했습니다.", e);
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
    public BestTourListResponse bestTourList(String lang) {

        try {
            List<BestTourListData> BestTourList = trmaHomeMapper.bestTourList();
            applyBestTourTranslations(BestTourList, lang);

            return new BestTourListResponse(
                    true,
                    200,
                    "SUCCESS",
                    "인기 관광지를 정상적으로 조회했습니다.",
                    "/trmaHome/tourCategory",
                    "",
                    BestTourList
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
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
    public BestMoimListResponse bestMoimList(String lang) {

        try {
            List<BestMoimListData> bestMoimList = trmaHomeMapper.bestMoimList();
            applyBestMoimTranslations(bestMoimList, lang);

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
            log.error("처리 중 오류가 발생했습니다.", e);
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

    // ========================= 홈 화면 번역 =========================
    // 홈의 "인기 관광지"/"인기 모임"은 개수가 적어(4~8개) 캐시 없이 매번 번역해도 부담이
    // 크지 않다. 관광지명/소모임 제목은 각 서비스가 이미 제공하는 캐시 우선 번역기를
    // 재사용하고, 관광지 주소만 캐시 없는 범용 번역기로 그때그때 번역한다.
    private void applyBestTourTranslations(List<BestTourListData> tours, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (tours == null || tours.isEmpty()) return;

        List<String> tourIds = tours.stream().map(BestTourListData::getTourId).toList();
        Map<String, String> names = tourListService.translateTourNames(tourIds, lang);
        for (BestTourListData tour : tours) {
            String name = names.get(tour.getTourId());
            if (name != null && !name.isBlank()) tour.setTourNm(name);
        }

        Map<String, String> addrsInput = new LinkedHashMap<>();
        for (BestTourListData tour : tours) {
            addrsInput.put(tour.getTourId(), tour.getRoadAddr());
        }
        Map<String, String> addrs = tourListService.translateFreeTexts(addrsInput, lang);
        for (BestTourListData tour : tours) {
            String addr = addrs.get(tour.getTourId());
            if (addr != null && !addr.isBlank()) tour.setRoadAddr(addr);
        }
    }

    private void applyBestMoimTranslations(List<BestMoimListData> moims, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (moims == null || moims.isEmpty()) return;

        List<String> moimIds = moims.stream().map(BestMoimListData::getMoimId).toList();
        Map<String, String> titles = moimListService.translateMoimTitles(moimIds, lang);
        for (BestMoimListData moim : moims) {
            String title = titles.get(moim.getMoimId());
            if (title != null && !title.isBlank()) moim.setMoimTitle(title);
        }
    }
}
