package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.request.CreateSafetyReportRequest;
import com.example.backend.trma.dto.response.CreateSafetyReportResponse;
import com.example.backend.trma.mapper.SafetyReportMapper;
import com.example.backend.trma.service.SafetyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyReportServiceImpl implements SafetyReportService {

    private final SafetyReportMapper safetyReportMapper;

    //안심신고 등록
    public CreateSafetyReportResponse createSafetyReport(CreateSafetyReportRequest request, String userId) {

        try {
            safetyReportMapper.insertSafetyReport(request, userId);

            return new CreateSafetyReportResponse(
                    true,
                    200,
                    "SUCCESS",
                    "신고가 접수되었습니다.",
                    "/safety-reports",
                    ""
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new CreateSafetyReportResponse(
                    false,
                    500,
                    "FAIL",
                    "신고 접수 중 오류가 발생했습니다.",
                    "/safety-reports",
                    ""
            );
        }
    }
}
