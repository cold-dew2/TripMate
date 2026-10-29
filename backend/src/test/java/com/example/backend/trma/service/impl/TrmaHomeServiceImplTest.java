package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.BestTourListData;
import com.example.backend.trma.dto.response.BestTourListResponse;
import com.example.backend.trma.mapper.TourListMapper;
import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.mapper.TrmaHomeMapper;
import com.example.backend.trma.service.TourTranslationService;
import com.example.backend.trma.service.MoimListService;
import com.example.backend.trma.service.TourListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[홈] TrmaHomeService - 인기 관광지")
class TrmaHomeServiceImplTest {

    @Mock TrmaHomeMapper trmaHomeMapper;
    @Mock TourListMapper tourListMapper;
    @Mock TourListService tourListService;
    @Mock MoimListService moimListService;
    @Mock TourTranslationMapper translationMapper;
    TrmaHomeServicelmpl service;

    @BeforeEach
    void setUp() {
        service = new TrmaHomeServicelmpl(trmaHomeMapper, tourListMapper, tourListService, moimListService,
                new TourTranslationService(translationMapper));
    }

    static BestTourListData tour(String id, String nm, String addr, String addrEn) {
        BestTourListData t = new BestTourListData();
        t.setTourId(id);
        t.setTourNm(nm);
        t.setRoadAddr(addr);
        t.setRoadAddrEn(addrEn);
        return t;
    }

    @Test
    @DisplayName("이름은 번역 서비스 결과, 주소는 수동 번역 캐시를 우선 사용한다")
    void usesCachedAddress() {
        when(trmaHomeMapper.bestTourList()).thenReturn(List.of(tour("T1", "경복궁", "서울 종로구", "Jongno-gu, Seoul")));
        when(tourListService.translateTourNames(any(), eq("en"))).thenReturn(Map.of("T1", "Gyeongbokgung"));

        BestTourListResponse response = service.bestTourList("en");

        BestTourListData t = response.getData().get(0);
        assertThat(t.getTourNm()).isEqualTo("Gyeongbokgung");
        assertThat(t.getRoadAddr()).isEqualTo("Jongno-gu, Seoul");
        verify(tourListService, never()).translateFreeTexts(any(), any());
    }

    @Test
    @DisplayName("주소 캐시가 없으면 Gemini 번역 후 저장, 한글이 남은 결과는 버린다")
    void translatesMissingAddress() {
        when(trmaHomeMapper.bestTourList()).thenReturn(List.of(
                tour("T1", "경복궁", "서울 종로구", null),
                tour("T2", "해운대", "부산 해운대구", null)));
        when(tourListService.translateFreeTexts(any(), eq("en")))
                .thenReturn(Map.of("T1", "Jongno-gu, Seoul", "T2", "부산 해운대구"));

        List<BestTourListData> data = service.bestTourList("en").getData();

        assertThat(data.get(0).getRoadAddr()).isEqualTo("Jongno-gu, Seoul");
        assertThat(data.get(1).getRoadAddr()).isEqualTo("부산 해운대구");
        verify(translationMapper).upsertTranslation("T1", "ROAD_ADDR", "en", "AI", "서울 종로구", "Jongno-gu, Seoul");
        verify(translationMapper, never()).upsertTranslation(eq("T2"), any(), any(), any(), any(), any());
        verify(translationMapper).resolveTourColumns(List.of("T1"));
    }

    @Test
    @DisplayName("주소 캐시 저장이 실패해도 번역된 주소를 보여준다")
    void saveFailure() {
        when(trmaHomeMapper.bestTourList()).thenReturn(List.of(tour("T1", "경복궁", "서울 종로구", null)));
        when(tourListService.translateFreeTexts(any(), eq("en"))).thenReturn(Map.of("T1", "Jongno-gu, Seoul"));
        when(translationMapper.upsertTranslation(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("db down"));

        assertThat(service.bestTourList("en").getData().get(0).getRoadAddr()).isEqualTo("Jongno-gu, Seoul");
    }

    @Test
    @DisplayName("한국어 요청이면 번역하지 않는다")
    void korean() {
        when(trmaHomeMapper.bestTourList()).thenReturn(List.of(tour("T1", "경복궁", "서울 종로구", null)));

        assertThat(service.bestTourList("ko").getData().get(0).getTourNm()).isEqualTo("경복궁");
        verifyNoInteractions(tourListService);
    }
}
