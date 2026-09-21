package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.BestTourListData;
import com.example.backend.trma.dto.dataList.CategoryInfoData;
import com.example.backend.trma.dto.dataList.UserInfoData;
import com.example.backend.trma.dto.dataList.BestMoimListData;
import com.example.backend.trma.dto.request.TourCategoryRequest;
import com.example.backend.trma.dto.request.UserInfoRequest;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.TourListMapper;
import com.example.backend.trma.mapper.TrmaHomeMapper;
import com.example.backend.trma.service.MoimListService;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.service.TrmaHomeService;
import com.example.backend.trma.util.AiJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrmaHomeServicelmpl implements TrmaHomeService {

    private final TrmaHomeMapper trmaHomeMapper;
    private final TourListMapper tourListMapper;
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
            // 홈 화면은 가장 먼저 보이는 화면이라 번역 대기를 최대 0.5초로 제한한다.
            // 못 끝나면 한국어로라도 바로 보여주고, 번역은 백그라운드에서 계속 돌아
            // 캐시에 저장돼 다음부터는 즉시 나온다.
            try {
                CompletableFuture.runAsync(() -> applyBestTourTranslations(BestTourList, lang))
                        .get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("인기 관광지 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. lang={}", lang);
            } catch (Exception e) {
                log.warn("인기 관광지 번역 대기 중 오류가 발생했습니다. lang={}", lang, e);
            }

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
            try {
                CompletableFuture.runAsync(() -> applyBestMoimTranslations(bestMoimList, lang))
                        .get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("인기 모임 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. lang={}", lang);
            } catch (Exception e) {
                log.warn("인기 모임 번역 대기 중 오류가 발생했습니다. lang={}", lang, e);
            }

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
    // 관광지명/소모임 제목은 각 서비스가 이미 제공하는 캐시 우선 번역기를 재사용한다.
    // 주소도 ROAD_ADDR_EN/JA에 캐시해 재사용한다 — 인기 관광지는 홈 화면에 계속 다시
    // 노출되므로(요청마다 매번) 캐시 없이 매번 Gemini를 부르면 홈 로딩이 눈에 띄게
    // 느려진다(관광지 목록의 주소 번역과 동일한 문제였다).
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
            String cached = "en".equals(lang) ? tour.getRoadAddrEn() : tour.getRoadAddrJa();
            if (cached != null && !cached.isBlank() && !AiJsonUtil.containsHangul(cached)) {
                tour.setRoadAddr(cached);
            } else {
                addrsInput.put(tour.getTourId(), tour.getRoadAddr());
            }
        }
        if (addrsInput.isEmpty()) return;

        Map<String, String> addrs = tourListService.translateFreeTexts(addrsInput, lang);
        for (BestTourListData tour : tours) {
            String addr = addrs.get(tour.getTourId());
            if (addr == null || addr.isBlank()) continue;

            tourListMapper.updateTourTranslation(
                    tour.getTourId(),
                    null,
                    null,
                    null,
                    null,
                    "en".equals(lang) ? addr : null,
                    "ja".equals(lang) ? addr : null,
                    null,
                    null
            );
            tour.setRoadAddr(addr);
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

        Map<String, String> dscrs = moimListService.translateMoimDescriptions(moimIds, lang);
        for (BestMoimListData moim : moims) {
            String dscr = dscrs.get(moim.getMoimId());
            if (dscr != null && !dscr.isBlank()) moim.setMoimDscr(dscr);
        }
    }
}
