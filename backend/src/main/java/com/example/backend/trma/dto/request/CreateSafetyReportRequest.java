package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class CreateSafetyReportRequest {
    private String moimId;
    private String targetUserId;
    private String reportType;
    private String content;
}
