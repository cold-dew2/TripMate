package com.example.backend.trma.dto.dataList;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyProfileData {
    private String userNm;
    private String areaNm;
    private String description;
    private String profileImageUrl;
    private double rating;
    private int reviewCount;
    private int ongoingMoimCount;
    private int memberCount;
    private List<LanguageCardData> languages;
    private List<RecentReviewData> recentReviews;
}
