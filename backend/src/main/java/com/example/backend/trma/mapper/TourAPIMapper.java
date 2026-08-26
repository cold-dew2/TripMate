package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TourAPIMapper {
    ///공통코드: 구
    List<SsgListData> ssgList();
    ///한국관광공사_기초지자체 중심 관광지 정보 입력
    void areaBased(AreaBasedListData data);

    ///한국관광공사_관광정보 동기화 목록 조회
    void areaBasedList2(AreaBasedListData data);

    ///한국관광공사 ContentId PK 조회
    List<TourAPIPKData> tourAPIPK();

    ///한국관광공사_관광정보 동기화 목록 조회
    void detailCommon2(TourCmmData data);

    ///한국관광공사_행사 정보 조회
    void searchFestival2(FastivalData data);

    ///한국관광공사_관광 데이터 마스터
    void tourMaster();

    ///한국관광공사_관광 카테고리 마스터
    void tourCateMaster();

}
