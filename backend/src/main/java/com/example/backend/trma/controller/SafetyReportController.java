package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.CreateSafetyReportRequest;
import com.example.backend.trma.dto.response.CreateSafetyReportResponse;
import com.example.backend.trma.service.SafetyReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SafetyReportController {
    private final SafetyReportService safetyReportService;

    public SafetyReportController(SafetyReportService safetyReportService) {
        this.safetyReportService = safetyReportService;
    }

    //안심신고 등록
    @PostMapping("/safety-reports")
    public CreateSafetyReportResponse createSafetyReport(@RequestBody CreateSafetyReportRequest request,
                                                          Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new CreateSafetyReportResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/safety-reports",
                    ""
            );
        }

        String userId = authentication.getName();
        return safetyReportService.createSafetyReport(request, userId);
    }
}
