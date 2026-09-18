package com.example.backend.trma.scheduler;

import com.example.backend.trma.dto.response.TourAPIResponse;
import com.example.backend.trma.service.TourAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 한국관광공사 OpenAPI는 실시간으로 호출하지 않고, 매일 새벽에 한 번씩
 * areaBased(목록) → detailCommon(개요/좌표) → searchFestival(축제) → tourMaster(운영 테이블 반영)
 * 순서로 배치 동기화해 TB_TRMA_TOUR_LIST를 최신 상태로 유지한다.
 * (기존에는 Swagger 등에서 각 배치 엔드포인트를 수동 호출해야만 갱신되었다.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TourApiSyncScheduler {

    private final TourAPIService tourAPIService;

    @Scheduled(cron = "0 0 3 * * *")
    public void syncTourData() {
        log.info("한국관광공사 관광지 데이터 배치 동기화를 시작합니다.");

        runStep("areaBased_batch", tourAPIService::areaBased_batch);
        runStep("detailCommon_batch", tourAPIService::detailCommon_batch);
        runStep("searchFestival_batch", tourAPIService::searchFestival_batch);
        runStep("tourMaster_batch", tourAPIService::tourMaster_batch);

        log.info("한국관광공사 관광지 데이터 배치 동기화를 종료합니다.");
    }

    private void runStep(String name, java.util.function.Supplier<TourAPIResponse> step) {
        try {
            TourAPIResponse response = step.get();
            if (response == null || !response.isSuccess()) {
                log.warn("{} 배치가 실패했습니다: {}", name, response == null ? "응답 없음" : response.getMessage());
            }
        } catch (Exception e) {
            log.error("{} 배치 실행 중 예외가 발생했습니다.", name, e);
        }
    }
}
