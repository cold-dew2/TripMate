package com.example.backend.trma.dto.request;

import com.example.backend.trma.dto.dataList.MoimCateData;
import com.example.backend.trma.dto.dataList.MoimPlanInsertData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@Setter
public class CreateMoimRequest {

    private String moimTitle;
    private String moimDscr;
    private String moimStartDt;
    private String moimEndDt;
    private int maxMember;
    private String moimImgUrl;

    private List<MoimCateData> moimCateData;
    private List<MoimPlanInsertData> moimPlanData;
}