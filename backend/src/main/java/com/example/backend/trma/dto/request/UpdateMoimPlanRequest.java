package com.example.backend.trma.dto.request;

import com.example.backend.trma.dto.dataList.MoimPlanInsertData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
public class UpdateMoimPlanRequest {

    private List<MoimPlanInsertData> items;
    private String moimEndDt;
}
