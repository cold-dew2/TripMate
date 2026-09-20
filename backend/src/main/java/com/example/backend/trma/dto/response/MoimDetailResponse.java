package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MoimDetailResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private MoimDetailData data;
    private List<MoimCateData> cate;
    private List<MoimPlanData> plan;
    private MoimJoinStatusData joinStatus;
    private MoimReviewStatusData reviewStatus;
}
