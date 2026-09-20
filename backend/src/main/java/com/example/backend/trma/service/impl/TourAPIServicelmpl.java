package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.response.SignupResponse;
import com.example.backend.trma.dto.response.TourAPIResponse;
import com.example.backend.trma.mapper.TourAPIMapper;
import com.example.backend.trma.service.TourAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourAPIServicelmpl implements TourAPIService {

    private final TourAPIMapper tourAPIMapper;
    private final RestClient restClient;

    //한국관광공사_URL
    @Value("${tour.api.host}")
    private String tourUrl;

    //기초지자체 중심 관광지 정보
    @Value("${tour.api.areaBasedList.path}")
    private String areaBasedListPath;
    @Value("${tour.api.areaBasedList.key}")
    private String areaBasedListKey;

    String localDate = LocalDate.now()
            .minusMonths(1)
            .withDayOfMonth(1)
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

//    @Override
//    public TourAPIResponse areaBasedList() {
//
//        List<SsgListData> SGG_CD = tourAPIMapper.ssgList();
//
//        try {
//            for (SsgListData sggCd : SGG_CD) {
//
//                int areaCd = Integer.parseInt(sggCd.getSggCd().substring(0, 2));
//                int signguCd = Integer.parseInt(sggCd.getSggCd().substring(0, 5));
//
//                String host = tourUrl;
//                String path = areaBasedListPath;
//                String apiKey = areaBasedListKey;
//
//                int numOfRows = 100;
//                int pageNo = 1;
//
//                int firstPageNo = pageNo;
//                JsonNode response = restClient.get()
//                        .uri(uriBuilder -> uriBuilder
//                            .scheme("https")
//                            .host(host)
//                            .path(path)
//                            .queryParam("serviceKey", apiKey)
//                            .queryParam("numOfRows", numOfRows)
//                            .queryParam("pageNo", firstPageNo)
//                            .queryParam("MobileOS", "WEB")
//                            .queryParam("MobileApp", "tripmate")
//                            .queryParam("_type", "json")
//                            .queryParam("baseYm", baseYm)
//                            .queryParam("areaCd", areaCd)
//                            .queryParam("signguCd", signguCd)
//                            .build())
//                        .retrieve()
//                        .body(JsonNode.class);
//
//                int totalCount = response.path("response")
//                        .path("body")
//                        .path("totalCount")
//                        .asInt();
//
//                int totalPage = (int) Math.ceil((double) totalCount / numOfRows);
//
//                processItems(response);
//
//                for (pageNo = 2; pageNo <= totalPage; pageNo++) {
//
//                    int nextPageNo = pageNo;
//
//                    response = restClient.get()
//                            .uri(uriBuilder -> uriBuilder
//                                    .scheme("https")
//                                    .host(host)
//                                    .path(path)
//                                    .queryParam("serviceKey", apiKey)
//                                    .queryParam("numOfRows", numOfRows)
//                                    .queryParam("pageNo", nextPageNo)
//                                    .queryParam("MobileOS", "WEB")
//                                    .queryParam("MobileApp", "tripmate")
//                                    .queryParam("_type", "json")
//                                    .queryParam("baseYm", baseYm)
//                                    .queryParam("areaCd", areaCd)
//                                    .queryParam("signguCd", signguCd)
//                                    .build())
//                            .retrieve()
//                            .body(JsonNode.class);
//
//                    processItems(response);
//                }
//
//            }
//            return new TourAPIResponse(
//                    true,
//                    200,
//                    "SUCCESS",
//                    "한국관광공사 API를 정상적으로 호출했습니다.",
//                    "/tourAPI/areaBasedList",
//                    null
//            );
//        } catch (Exception e) {
//            return new TourAPIResponse(
//                    false,
//                    500,
//                    "FAIL",
//                    "한국관광공사 API 호출중 오류가 발생했습니다.",
//                    "/tourAPI/areaBasedList",
//                    null
//            );
//        }
//    }
//
//    private void processItems(JsonNode response) {
//
//        JsonNode items = response.path("response")
//                .path("body")
//                .path("items")
//                .path("item");
//
//        for (JsonNode item : items) {
//            AreaBasedListData data = new AreaBasedListData();
//            data.setBaseYm(item.path("baseYm").asString());
//            data.setMapX(item.path("mapX").asString());
//            data.setMapY(item.path("mapY").asString());
//            data.setAreaCd(item.path("areaCd").asString());
//            data.setAreaNm(item.path("areaNm").asString());
//            data.setSignguCd(item.path("signguCd").asString());
//            data.setSignguNm(item.path("signguNm").asString());
//            data.setHubTatsCd(item.path("hubTatsCd").asString());
//            data.setHubTatsNm(item.path("hubTatsNm").asString());
//            data.setHubCtgryLclsNm(item.path("hubCtgryLclsNm").asString());
//            data.setHubCtgryMclsNm(item.path("hubCtgryMclsNm").asString());
//            data.setHubRank(item.path("hubRank").asInt());
//
//            tourAPIMapper.areaBased(data);
//        }
//    }



    //기초지자체 중심 관광지 정보
    @Value("${tour.api.key}")
    private String ServiceKey;
    @Value("${tour.api.KorService.detailCommon.path}")
    private String DetailCommonPath;
    @Value("${tour.api.KorService.areaBasedList.path}")
    private String AreaBasedListPath;
    @Value("${tour.api.KorService.searchFestival.path}")
    private String SearchFestivalPath;

    @Value("${tour.api.JpnService.detailCommon.path}")
    private String DetailCommonPath_JP;
    @Value("${tour.api.JpnService.areaBasedList.path}")
    private String AreaBasedListPath_JP;
    @Value("${tour.api.JpnService.searchFestival.path}")
    private String SearchFestivalPath_JP;

    @Value("${tour.api.EngService.detailCommon.path}")
    private String DetailCommonPath_EN;
    @Value("${tour.api.EngService.areaBasedList.path}")
    private String AreaBasedListPath_EN;
    @Value("${tour.api.EngService.searchFestival.path}")
    private String SearchFestivalPath_EN;

    @Override
    public TourAPIResponse areaBased_batch() {
        try {
            //areaBasedList2();
            //areaBasedList2_jp();
            areaBasedList2_en();

            return new TourAPIResponse(
                    true,
                    200,
                    "SUCCESS",
                    "한국관광공사 API를 정상적으로 호출했습니다.",
                    "/tourAPI/areaBasedList2",
                    null
            );
        } catch (Exception e) {
            log.error("한국관광공사 areaBasedList2 호출 중 오류가 발생했습니다.", e);

            return new TourAPIResponse(
                    false,
                    500,
                    "FAIL",
                    "한국관광공사 API 호출 오류: " + e,
                    "/tourAPI/areaBasedList2",
                    null
            );
        }
    }

    @Override
    public TourAPIResponse detailCommon_batch() {
        try {
            //detailCommon2();
            //detailCommon2_jp();
            detailCommon2_en();

            return new TourAPIResponse(
                    true,
                    200,
                    "SUCCESS",
                    "한국관광공사 API를 정상적으로 호출했습니다.",
                    "/tourAPI/detailCommon2",
                    null
            );
        } catch (Exception e) {
            log.error("한국관광공사 detailCommon2 호출 중 오류가 발생했습니다.", e);

            return new TourAPIResponse(
                    false,
                    500,
                    "FAIL",
                    "한국관광공사 API 호출 오류: " + e,
                    "/tourAPI/detailCommon2",
                    null
            );
        }
    }

    @Override
    public TourAPIResponse searchFestival_batch() {
        try {
            searchFestival2();
            searchFestival2_jp();
            searchFestival2_en();

            return new TourAPIResponse(
                    true,
                    200,
                    "SUCCESS",
                    "한국관광공사 API를 정상적으로 호출했습니다.",
                    "/tourAPI/searchFestival2",
                    null
            );
        } catch (Exception e) {
            log.error("한국관광공사 searchFestival2 호출 중 오류가 발생했습니다.", e);

            return new TourAPIResponse(
                    false,
                    500,
                    "FAIL",
                    "한국관광공사 API 호출 오류: " + e,
                    "/tourAPI/searchFestival2",
                    null
            );
        }
    }

    @Override
    public TourAPIResponse tourMaster_batch() {
        try {
            tourMaster();
            tourMaster_jp();
            tourMaster_en();

            return new TourAPIResponse(
                    true,
                    200,
                    "SUCCESS",
                    "한국관광공사 API를 정상적으로 호출했습니다.",
                    "/tourAPI/tourMaster",
                    null
            );
        } catch (Exception e) {
            log.error("한국관광공사 tourMaster 배치 처리 중 오류가 발생했습니다.", e);

            return new TourAPIResponse(
                    false,
                    500,
                    "FAIL",
                    "한국관광공사 API 호출 오류: " + e,
                    "/tourAPI/tourMaster",
                    null
            );
        }
    }

    //지역기반 관광정보조회
    private void areaBasedList2() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = AreaBasedListPath;

        int numOfRows = 100;
        int pageNo = 1;

        int firstPageNo = pageNo;
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(host)
                        .path(path)
                        .queryParam("serviceKey", apiKey)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", firstPageNo)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "tripmate")
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        int totalCount = response.path("response")
                .path("body")
                .path("totalCount")
                .asInt();

        int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

        String resultCode = response.path("response")
                .path("header")
                .path("resultCode")
                .asString();

        if(Objects.equals(resultCode, "0000")) {
            areaBasedList2_insert(response);

            for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                int nextPageNo = pageNo;

                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host(host)
                                .path(path)
                                .queryParam("serviceKey", apiKey)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("pageNo", nextPageNo)
                                .queryParam("MobileOS", "WEB")
                                .queryParam("MobileApp", "tripmate")
                                .queryParam("_type", "json")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                resultCode = response.path("response")
                        .path("header")
                        .path("resultCode")
                        .asString();

                if(Objects.equals(resultCode, "0000")){
                    areaBasedList2_insert(response);
                }
            }
        }
    }

    //지역기반 관광정보조회
    private void areaBasedList2_jp() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = AreaBasedListPath_JP;

        int numOfRows = 100;
        int pageNo = 1;

        int firstPageNo = pageNo;
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(host)
                        .path(path)
                        .queryParam("serviceKey", apiKey)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", firstPageNo)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "tripmate")
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        int totalCount = response.path("response")
                .path("body")
                .path("totalCount")
                .asInt();

        int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

        String resultCode = response.path("response")
                .path("header")
                .path("resultCode")
                .asString();

        if(Objects.equals(resultCode, "0000")) {
            areaBasedList2_jp_insert(response);

            for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                int nextPageNo = pageNo;

                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host(host)
                                .path(path)
                                .queryParam("serviceKey", apiKey)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("pageNo", nextPageNo)
                                .queryParam("MobileOS", "WEB")
                                .queryParam("MobileApp", "tripmate")
                                .queryParam("_type", "json")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                resultCode = response.path("response")
                        .path("header")
                        .path("resultCode")
                        .asString();

                if(Objects.equals(resultCode, "0000")){
                    areaBasedList2_jp_insert(response);
                }
            }
        }
    }

    //지역기반 관광정보조회
    private void areaBasedList2_en() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = AreaBasedListPath_EN;

        int numOfRows = 100;
        int pageNo = 1;

        int firstPageNo = pageNo;
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(host)
                        .path(path)
                        .queryParam("serviceKey", apiKey)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", firstPageNo)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "tripmate")
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        int totalCount = response.path("response")
                .path("body")
                .path("totalCount")
                .asInt();

        int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

        String resultCode = response.path("response")
                .path("header")
                .path("resultCode")
                .asString();

        if(Objects.equals(resultCode, "0000")) {
            areaBasedList2_en_insert(response);

            for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                int nextPageNo = pageNo;

                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host(host)
                                .path(path)
                                .queryParam("serviceKey", apiKey)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("pageNo", nextPageNo)
                                .queryParam("MobileOS", "WEB")
                                .queryParam("MobileApp", "tripmate")
                                .queryParam("_type", "json")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                resultCode = response.path("response")
                        .path("header")
                        .path("resultCode")
                        .asString();

                if(Objects.equals(resultCode, "0000")){
                    areaBasedList2_en_insert(response);
                }
            }
        }
    }

    //지역기반 관광정보 입력
    private void areaBasedList2_jp_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        for (JsonNode item : items) {
            if(!Objects.equals(item.path("contenttypeid").asString(), "32")){
                AreaBasedListData data = new AreaBasedListData();
                data.setContentid(item.path("contentid").asString());
                data.setModifiedtime(item.path("modifiedtime").asString());
                data.setTitle(item.path("title").asString());
                data.setContenttypeid(item.path("contenttypeid").asString());
                data.setLclsSystm1(item.path("lclsSystm1").asString());
                data.setLclsSystm2(item.path("lclsSystm2").asString());
                data.setLclsSystm3(item.path("lclsSystm3").asString());
                data.setLDongRegnCd(item.path("lDongRegnCd").asString());
                data.setLDongSignguCd(item.path("lDongSignguCd").asString());
                data.setFirstimage(item.path("firstimage").asString());
                data.setFirstimage2(item.path("firstimage2").asString());
                data.setAddr1(item.path("addr1").asString());
                data.setAddr2(item.path("addr2").asString());
                data.setZipcode(item.path("zipcode").asString());
                data.setMapx(item.path("mapx").asString());
                data.setMapy(item.path("mapy").asString());

                tourAPIMapper.areaBasedList2_jp(data);
            }
        }
    }

    //지역기반 관광정보 입력
    private void areaBasedList2_en_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        for (JsonNode item : items) {
            if(!Objects.equals(item.path("contenttypeid").asString(), "32")){
                AreaBasedListData data = new AreaBasedListData();
                data.setContentid(item.path("contentid").asString());
                data.setModifiedtime(item.path("modifiedtime").asString());
                data.setTitle(item.path("title").asString());
                data.setContenttypeid(item.path("contenttypeid").asString());
                data.setLclsSystm1(item.path("lclsSystm1").asString());
                data.setLclsSystm2(item.path("lclsSystm2").asString());
                data.setLclsSystm3(item.path("lclsSystm3").asString());
                data.setLDongRegnCd(item.path("lDongRegnCd").asString());
                data.setLDongSignguCd(item.path("lDongSignguCd").asString());
                data.setFirstimage(item.path("firstimage").asString());
                data.setFirstimage2(item.path("firstimage2").asString());
                data.setAddr1(item.path("addr1").asString());
                data.setAddr2(item.path("addr2").asString());
                data.setZipcode(item.path("zipcode").asString());
                data.setMapx(item.path("mapx").asString());
                data.setMapy(item.path("mapy").asString());

                tourAPIMapper.areaBasedList2_en(data);
            }
        }
    }

    //지역기반 관광정보 입력
    private void areaBasedList2_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        for (JsonNode item : items) {
            if(!Objects.equals(item.path("contenttypeid").asString(), "32")){
                AreaBasedListData data = new AreaBasedListData();
                data.setContentid(item.path("contentid").asString());
                data.setModifiedtime(item.path("modifiedtime").asString());
                data.setTitle(item.path("title").asString());
                data.setContenttypeid(item.path("contenttypeid").asString());
                data.setLclsSystm1(item.path("lclsSystm1").asString());
                data.setLclsSystm2(item.path("lclsSystm2").asString());
                data.setLclsSystm3(item.path("lclsSystm3").asString());
                data.setLDongRegnCd(item.path("lDongRegnCd").asString());
                data.setLDongSignguCd(item.path("lDongSignguCd").asString());
                data.setFirstimage(item.path("firstimage").asString());
                data.setFirstimage2(item.path("firstimage2").asString());
                data.setAddr1(item.path("addr1").asString());
                data.setAddr2(item.path("addr2").asString());
                data.setZipcode(item.path("zipcode").asString());
                data.setMapx(item.path("mapx").asString());
                data.setMapy(item.path("mapy").asString());

                tourAPIMapper.areaBasedList2(data);
            }
        }
    }

    //관광공통정보조회
    private void detailCommon2() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = DetailCommonPath;

        List<TourAPIPKData> Pks = tourAPIMapper.tourAPIPK();

        for (TourAPIPKData Pk : Pks) {
            String contentId = Pk.getContentid();

            int numOfRows = 100;
            int pageNo = 1;

            int firstPageNo = pageNo;

            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(host)
                            .path(path)
                            .queryParam("serviceKey", apiKey)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("pageNo", firstPageNo)
                            .queryParam("MobileOS", "WEB")
                            .queryParam("MobileApp", "tripmate")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            String resultCode = response.path("response")
                    .path("header")
                    .path("resultCode")
                    .asString();

            if(Objects.equals(resultCode, "0000")) {

                JsonNode items = response.path("response")
                        .path("body")
                        .path("items")
                        .path("item");

                int totalCount = response.path("response")
                        .path("body")
                        .path("totalCount")
                        .asInt();

                int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

                detailCommon2_insert(response);

                for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                    int nextPageNo = pageNo;

                    response = restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .scheme("https")
                                    .host(host)
                                    .path(path)
                                    .queryParam("serviceKey", apiKey)
                                    .queryParam("numOfRows", numOfRows)
                                    .queryParam("pageNo", nextPageNo)
                                    .queryParam("MobileOS", "WEB")
                                    .queryParam("MobileApp", "tripmate")
                                    .queryParam("_type", "json")
                                    .queryParam("contentId", contentId)
                                    .build())
                            .retrieve()
                            .body(JsonNode.class);

                    resultCode = response.path("response")
                            .path("header")
                            .path("resultCode")
                            .asString();

                    if(Objects.equals(resultCode, "0000")) {
                        detailCommon2_insert(response);
                    }
                }
            }
        }
    }

    //관광공통정보조회_일어
    private void detailCommon2_jp() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = DetailCommonPath_JP;

        List<TourAPIPKData> Pks = tourAPIMapper.tourAPIPK();

        for (TourAPIPKData Pk : Pks) {
            String contentId = Pk.getContentid();

            int numOfRows = 100;
            int pageNo = 1;

            int firstPageNo = pageNo;

            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(host)
                            .path(path)
                            .queryParam("serviceKey", apiKey)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("pageNo", firstPageNo)
                            .queryParam("MobileOS", "WEB")
                            .queryParam("MobileApp", "tripmate")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            String resultCode = response.path("response")
                    .path("header")
                    .path("resultCode")
                    .asString();

            if(Objects.equals(resultCode, "0000")) {

                JsonNode items = response.path("response")
                        .path("body")
                        .path("items")
                        .path("item");

                int totalCount = response.path("response")
                        .path("body")
                        .path("totalCount")
                        .asInt();

                int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

                detailCommon2_jp_insert(response);

                for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                    int nextPageNo = pageNo;

                    response = restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .scheme("https")
                                    .host(host)
                                    .path(path)
                                    .queryParam("serviceKey", apiKey)
                                    .queryParam("numOfRows", numOfRows)
                                    .queryParam("pageNo", nextPageNo)
                                    .queryParam("MobileOS", "WEB")
                                    .queryParam("MobileApp", "tripmate")
                                    .queryParam("_type", "json")
                                    .queryParam("contentId", contentId)
                                    .build())
                            .retrieve()
                            .body(JsonNode.class);

                    resultCode = response.path("response")
                            .path("header")
                            .path("resultCode")
                            .asString();

                    if(Objects.equals(resultCode, "0000")) {
                        detailCommon2_jp_insert(response);
                    }
                }
            }
        }
    }

    //관광공통정보조회_영문
    private void detailCommon2_en() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = DetailCommonPath_EN;

        List<TourAPIPKData> Pks = tourAPIMapper.tourAPIPK();

        for (TourAPIPKData Pk : Pks) {
            String contentId = Pk.getContentid();

            int numOfRows = 100;
            int pageNo = 1;

            int firstPageNo = pageNo;

            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(host)
                            .path(path)
                            .queryParam("serviceKey", apiKey)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("pageNo", firstPageNo)
                            .queryParam("MobileOS", "WEB")
                            .queryParam("MobileApp", "tripmate")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            String resultCode = response.path("response")
                    .path("header")
                    .path("resultCode")
                    .asString();

            if(Objects.equals(resultCode, "0000")) {

                JsonNode items = response.path("response")
                        .path("body")
                        .path("items")
                        .path("item");

                int totalCount = response.path("response")
                        .path("body")
                        .path("totalCount")
                        .asInt();

                int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

                detailCommon2_en_insert(response);

                for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                    int nextPageNo = pageNo;

                    response = restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .scheme("https")
                                    .host(host)
                                    .path(path)
                                    .queryParam("serviceKey", apiKey)
                                    .queryParam("numOfRows", numOfRows)
                                    .queryParam("pageNo", nextPageNo)
                                    .queryParam("MobileOS", "WEB")
                                    .queryParam("MobileApp", "tripmate")
                                    .queryParam("_type", "json")
                                    .queryParam("contentId", contentId)
                                    .build())
                            .retrieve()
                            .body(JsonNode.class);

                    resultCode = response.path("response")
                            .path("header")
                            .path("resultCode")
                            .asString();

                    if(Objects.equals(resultCode, "0000")) {
                        detailCommon2_en_insert(response);
                    }
                }
            }
        }
    }

    //관광공통정보 입력
    private void detailCommon2_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        System.out.println("items: " + items);

        for (JsonNode item : items) {
            TourCmmData data = new TourCmmData();
            data.setContentId(item.path("contentid").asString());
            data.setContentTypeId(item.path("contenttypeid").asString());
            data.setTitle(item.path("title").asString());
            data.setCreatedTime(item.path("createdtime").asString());
            data.setModifiedTime(item.path("modifiedtime").asString());
            data.setHomepage(item.path("homepage").asString());
            data.setTel(item.path("tel").asString());
            data.setTelName(item.path("telname").asString());
            data.setFirstImage(item.path("firstimage").asString());
            data.setFirstImage2(item.path("firstimage2").asString());
            data.setCpyrhtDivCd(item.path("cpyrhtDivCd").asString());
            data.setAreaCode(item.path("areacode").asString());
            data.setSigunguCode(item.path("sigungucode").asString());
            data.setLDongRegnCd(item.path("lDongRegnCd").asString());
            data.setLDongSignguCd(item.path("lDongSignguCd").asString());
            data.setLclsSystm1(item.path("lclsSystm1").asString());
            data.setLclsSystm2(item.path("lclsSystm2").asString());
            data.setLclsSystm3(item.path("lclsSystm3").asString());
            data.setCat1(item.path("cat1").asString());
            data.setCat2(item.path("cat2").asString());
            data.setCat3(item.path("cat3").asString());
            data.setAddr1(item.path("addr1").asString());
            data.setAddr2(item.path("addr2").asString());
            data.setZipcode(item.path("zipcode").asString());
            data.setMapX(item.path("mapx").asString());
            data.setMapY(item.path("mapy").asString());
            data.setMLevel(item.path("mlevel").asString());
            data.setOverview(item.path("overview").asString());

            tourAPIMapper.detailCommon2(data);
        }
    }

    //관광공통정보 입력
    private void detailCommon2_jp_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        System.out.println("items: " + items);

        for (JsonNode item : items) {
            TourCmmData data = new TourCmmData();
            data.setContentId(item.path("contentid").asString());
            data.setContentTypeId(item.path("contenttypeid").asString());
            data.setTitle(item.path("title").asString());
            data.setCreatedTime(item.path("createdtime").asString());
            data.setModifiedTime(item.path("modifiedtime").asString());
            data.setHomepage(item.path("homepage").asString());
            data.setTel(item.path("tel").asString());
            data.setTelName(item.path("telname").asString());
            data.setFirstImage(item.path("firstimage").asString());
            data.setFirstImage2(item.path("firstimage2").asString());
            data.setCpyrhtDivCd(item.path("cpyrhtDivCd").asString());
            data.setAreaCode(item.path("areacode").asString());
            data.setSigunguCode(item.path("sigungucode").asString());
            data.setLDongRegnCd(item.path("lDongRegnCd").asString());
            data.setLDongSignguCd(item.path("lDongSignguCd").asString());
            data.setLclsSystm1(item.path("lclsSystm1").asString());
            data.setLclsSystm2(item.path("lclsSystm2").asString());
            data.setLclsSystm3(item.path("lclsSystm3").asString());
            data.setCat1(item.path("cat1").asString());
            data.setCat2(item.path("cat2").asString());
            data.setCat3(item.path("cat3").asString());
            data.setAddr1(item.path("addr1").asString());
            data.setAddr2(item.path("addr2").asString());
            data.setZipcode(item.path("zipcode").asString());
            data.setMapX(item.path("mapx").asString());
            data.setMapY(item.path("mapy").asString());
            data.setMLevel(item.path("mlevel").asString());
            data.setOverview(item.path("overview").asString());

            tourAPIMapper.detailCommon2_jp(data);
        }
    }

    //관광공통정보 입력
    private void detailCommon2_en_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        System.out.println("items: " + items);

        for (JsonNode item : items) {
            TourCmmData data = new TourCmmData();
            data.setContentId(item.path("contentid").asString());
            data.setContentTypeId(item.path("contenttypeid").asString());
            data.setTitle(item.path("title").asString());
            data.setCreatedTime(item.path("createdtime").asString());
            data.setModifiedTime(item.path("modifiedtime").asString());
            data.setHomepage(item.path("homepage").asString());
            data.setTel(item.path("tel").asString());
            data.setTelName(item.path("telname").asString());
            data.setFirstImage(item.path("firstimage").asString());
            data.setFirstImage2(item.path("firstimage2").asString());
            data.setCpyrhtDivCd(item.path("cpyrhtDivCd").asString());
            data.setAreaCode(item.path("areacode").asString());
            data.setSigunguCode(item.path("sigungucode").asString());
            data.setLDongRegnCd(item.path("lDongRegnCd").asString());
            data.setLDongSignguCd(item.path("lDongSignguCd").asString());
            data.setLclsSystm1(item.path("lclsSystm1").asString());
            data.setLclsSystm2(item.path("lclsSystm2").asString());
            data.setLclsSystm3(item.path("lclsSystm3").asString());
            data.setCat1(item.path("cat1").asString());
            data.setCat2(item.path("cat2").asString());
            data.setCat3(item.path("cat3").asString());
            data.setAddr1(item.path("addr1").asString());
            data.setAddr2(item.path("addr2").asString());
            data.setZipcode(item.path("zipcode").asString());
            data.setMapX(item.path("mapx").asString());
            data.setMapY(item.path("mapy").asString());
            data.setMLevel(item.path("mlevel").asString());
            data.setOverview(item.path("overview").asString());

            tourAPIMapper.detailCommon2_en(data);
        }
    }

    //행사 정보 조회
    private void searchFestival2() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = SearchFestivalPath;

        int numOfRows = 100;
        int pageNo = 1;

        int firstPageNo = pageNo;
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(host)
                        .path(path)
                        .queryParam("serviceKey", apiKey)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", firstPageNo)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "tripmate")
                        .queryParam("_type", "json")
                        .queryParam("eventStartDate", localDate)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        String resultCode = response.path("response")
                .path("header")
                .path("resultCode")
                .asString();

        if(Objects.equals(resultCode, "0000")) {

            JsonNode items = response.path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            int totalCount = response.path("response")
                    .path("body")
                    .path("totalCount")
                    .asInt();

            int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

            searchFestival2_insert(response);

            for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                int nextPageNo = pageNo;

                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host(host)
                                .path(path)
                                .queryParam("serviceKey", apiKey)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("pageNo", nextPageNo)
                                .queryParam("MobileOS", "WEB")
                                .queryParam("MobileApp", "tripmate")
                                .queryParam("_type", "json")
                                .queryParam("eventStartDate", localDate)
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                resultCode = response.path("response")
                        .path("header")
                        .path("resultCode")
                        .asString();

                if(Objects.equals(resultCode, "0000")) {
                    searchFestival2_insert(response);
                }
            }
        }
    }

    //행사 정보 조회
    private void searchFestival2_jp() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = SearchFestivalPath_JP;

        int numOfRows = 100;
        int pageNo = 1;

        int firstPageNo = pageNo;
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(host)
                        .path(path)
                        .queryParam("serviceKey", apiKey)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", firstPageNo)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "tripmate")
                        .queryParam("_type", "json")
                        .queryParam("eventStartDate", localDate)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        String resultCode = response.path("response")
                .path("header")
                .path("resultCode")
                .asString();

        if(Objects.equals(resultCode, "0000")) {

            JsonNode items = response.path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            int totalCount = response.path("response")
                    .path("body")
                    .path("totalCount")
                    .asInt();

            int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

            searchFestival2_jp_insert(response);

            for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                int nextPageNo = pageNo;

                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host(host)
                                .path(path)
                                .queryParam("serviceKey", apiKey)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("pageNo", nextPageNo)
                                .queryParam("MobileOS", "WEB")
                                .queryParam("MobileApp", "tripmate")
                                .queryParam("_type", "json")
                                .queryParam("eventStartDate", localDate)
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                resultCode = response.path("response")
                        .path("header")
                        .path("resultCode")
                        .asString();

                if(Objects.equals(resultCode, "0000")) {
                    searchFestival2_jp_insert(response);
                }
            }
        }
    }

    //행사 정보 조회
    private void searchFestival2_en() {
        String host = tourUrl;
        String apiKey = ServiceKey;
        String path = SearchFestivalPath_EN;

        int numOfRows = 100;
        int pageNo = 1;

        int firstPageNo = pageNo;
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(host)
                        .path(path)
                        .queryParam("serviceKey", apiKey)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", firstPageNo)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "tripmate")
                        .queryParam("_type", "json")
                        .queryParam("eventStartDate", localDate)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        String resultCode = response.path("response")
                .path("header")
                .path("resultCode")
                .asString();

        if(Objects.equals(resultCode, "0000")) {

            JsonNode items = response.path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            int totalCount = response.path("response")
                    .path("body")
                    .path("totalCount")
                    .asInt();

            int totalPage = (int) Math.ceil((double) totalCount / numOfRows);

            searchFestival2_en_insert(response);

            for (pageNo = 2; pageNo <= totalPage; pageNo++) {

                int nextPageNo = pageNo;

                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host(host)
                                .path(path)
                                .queryParam("serviceKey", apiKey)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("pageNo", nextPageNo)
                                .queryParam("MobileOS", "WEB")
                                .queryParam("MobileApp", "tripmate")
                                .queryParam("_type", "json")
                                .queryParam("eventStartDate", localDate)
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                resultCode = response.path("response")
                        .path("header")
                        .path("resultCode")
                        .asString();

                if(Objects.equals(resultCode, "0000")) {
                    searchFestival2_en_insert(response);
                }
            }
        }
    }
    //행사 정보 입력
    private void searchFestival2_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        System.out.println("items: " + items);

        for (JsonNode item : items) {
            FastivalData data = new FastivalData();

            data.setContentId(item.path("contentid").asString());
            data.setContentTypeId(item.path("contenttypeid").asString());
            data.setTitle(item.path("title").asString());
            data.setCreatedTime(item.path("createdtime").asString());
            data.setModifiedTime(item.path("modifiedtime").asString());
            data.setEventStartDate(item.path("eventstartdate").asString());
            data.setEventEndDate(item.path("eventenddate").asString());
            data.setFirstImage(item.path("firstimage").asString());
            data.setFirstImage2(item.path("firstimage2").asString());
            data.setCpyrhtDivCd(item.path("cpyrhtDivCd").asString());
            data.setAddr1(item.path("addr1").asString());
            data.setAddr2(item.path("addr2").asString());
            data.setZipcode(item.path("zipcode").asString());
            data.setMapX(item.path("mapx").asString());
            data.setMapY(item.path("mapy").asString());
            data.setMLevel(item.path("mlevel").asString());
            data.setLDongRegnCd(item.path("lDongRegnCd").asString());
            data.setLDongSignguCd(item.path("lDongSignguCd").asString());
            data.setLclsSystm1(item.path("lclsSystm1").asString());
            data.setLclsSystm2(item.path("lclsSystm2").asString());
            data.setLclsSystm3(item.path("lclsSystm3").asString());
            data.setProgressType(item.path("progresstype").asString());
            data.setFestivalType(item.path("festivaltype").asString());

            tourAPIMapper.searchFestival2(data);
        }
    }

    //행사 정보 입력
    private void searchFestival2_jp_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        System.out.println("items: " + items);

        for (JsonNode item : items) {
            FastivalData data = new FastivalData();

            data.setContentId(item.path("contentid").asString());
            data.setContentTypeId(item.path("contenttypeid").asString());
            data.setTitle(item.path("title").asString());
            data.setCreatedTime(item.path("createdtime").asString());
            data.setModifiedTime(item.path("modifiedtime").asString());
            data.setEventStartDate(item.path("eventstartdate").asString());
            data.setEventEndDate(item.path("eventenddate").asString());
            data.setFirstImage(item.path("firstimage").asString());
            data.setFirstImage2(item.path("firstimage2").asString());
            data.setCpyrhtDivCd(item.path("cpyrhtDivCd").asString());
            data.setAddr1(item.path("addr1").asString());
            data.setAddr2(item.path("addr2").asString());
            data.setZipcode(item.path("zipcode").asString());
            data.setMapX(item.path("mapx").asString());
            data.setMapY(item.path("mapy").asString());
            data.setMLevel(item.path("mlevel").asString());
            data.setLDongRegnCd(item.path("lDongRegnCd").asString());
            data.setLDongSignguCd(item.path("lDongSignguCd").asString());
            data.setLclsSystm1(item.path("lclsSystm1").asString());
            data.setLclsSystm2(item.path("lclsSystm2").asString());
            data.setLclsSystm3(item.path("lclsSystm3").asString());
            data.setProgressType(item.path("progresstype").asString());
            data.setFestivalType(item.path("festivaltype").asString());

            tourAPIMapper.searchFestival2_jp(data);
        }
    }

    //행사 정보 입력
    private void searchFestival2_en_insert(JsonNode response){
        JsonNode items = response.path("response")
                .path("body")
                .path("items")
                .path("item");

        System.out.println("items: " + items);

        for (JsonNode item : items) {
            FastivalData data = new FastivalData();

            data.setContentId(item.path("contentid").asString());
            data.setContentTypeId(item.path("contenttypeid").asString());
            data.setTitle(item.path("title").asString());
            data.setCreatedTime(item.path("createdtime").asString());
            data.setModifiedTime(item.path("modifiedtime").asString());
            data.setEventStartDate(item.path("eventstartdate").asString());
            data.setEventEndDate(item.path("eventenddate").asString());
            data.setFirstImage(item.path("firstimage").asString());
            data.setFirstImage2(item.path("firstimage2").asString());
            data.setCpyrhtDivCd(item.path("cpyrhtDivCd").asString());
            data.setAddr1(item.path("addr1").asString());
            data.setAddr2(item.path("addr2").asString());
            data.setZipcode(item.path("zipcode").asString());
            data.setMapX(item.path("mapx").asString());
            data.setMapY(item.path("mapy").asString());
            data.setMLevel(item.path("mlevel").asString());
            data.setLDongRegnCd(item.path("lDongRegnCd").asString());
            data.setLDongSignguCd(item.path("lDongSignguCd").asString());
            data.setLclsSystm1(item.path("lclsSystm1").asString());
            data.setLclsSystm2(item.path("lclsSystm2").asString());
            data.setLclsSystm3(item.path("lclsSystm3").asString());
            data.setProgressType(item.path("progresstype").asString());
            data.setFestivalType(item.path("festivaltype").asString());

            tourAPIMapper.searchFestival2_en(data);
        }
    }

    //숙박 정보 조회
    private void tourMaster() {
        tourAPIMapper.tourMaster();
        tourAPIMapper.tourCateMaster();
    }

    //숙박 정보 조회
    private void tourMaster_jp() {
        tourAPIMapper.tourMaster_jp();
    }

    //숙박 정보 조회
    private void tourMaster_en() {
        tourAPIMapper.tourMaster_en();
    }
}
