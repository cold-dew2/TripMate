package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.CreateSafetyReportRequest;
import com.example.backend.trma.dto.response.CreateSafetyReportResponse;

public interface SafetyReportService {
    //안심신고 등록
    CreateSafetyReportResponse createSafetyReport(CreateSafetyReportRequest request, String userId);
}
