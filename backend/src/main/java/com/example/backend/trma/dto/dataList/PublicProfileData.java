package com.example.backend.trma.dto.dataList;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PublicProfileData {
    private String userId;
    private String userNm;
    private String areaNm;
    private String description;
    private String profileImageUrl;
    private double rating;
    private int reviewCount;
    private String joinDt;
    private List<LanguageCardData> languages;
}
