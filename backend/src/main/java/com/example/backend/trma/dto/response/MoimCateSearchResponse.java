package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.MoimCateData;
import com.example.backend.trma.dto.dataList.MoimSearchData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MoimCateSearchResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private List<MoimCateData> data;
}
