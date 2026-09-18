package com.example.backend.trma.dto.dataList;

import java.util.List;

public class GeminiData {

    private List<Recommendation> recommendations;

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public static class Recommendation {

        private String tourId;
        private String score;
        private String reason;

        public String getTourId() {
            return tourId;
        }

        public void setTourId(String tourId) {
            this.tourId = tourId;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    private List<Recommendation2> recommendations2;

    public List<Recommendation2> getRecommendations2() {
        return recommendations2;
    }

    public void setRecommendations2(List<Recommendation2> recommendations2) {
        this.recommendations2 = recommendations2;
    }

    public static class Recommendation2 {

        private String moimId;
        private String score;
        private String reason;

        public String getMoimId() {
            return moimId;
        }

        public void setMoimId(String tourId) {
            this.moimId = tourId;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    private List<Recommendation3> recommendations3;

    public List<Recommendation3> getRecommendations3() {
        return recommendations3;
    }

    public void setRecommendations3(List<Recommendation3> recommendations3) {
        this.recommendations3 = recommendations3;
    }

    public static class Recommendation3 {
        private String operatingHours;
        private String closedDays;
        private String admissionFeeIsFree;
        private String admissionFeeDetails;
        private String websiteUrl;
        private String parkingAvailable;
        private String parkingFeeInfo;
        private String lastUpdatedNote;

        public String getOperatingHours() {
            return operatingHours;
        }
        public void setOperatingHours(String tourId) {
            this.operatingHours = tourId;
        }

        public String getClosedDays() {
            return closedDays;
        }
        public void setClosedDays(String tourId) {
            this.closedDays = tourId;
        }

        public String getAdmissionFeeIsFree() {
            return admissionFeeIsFree;
        }
        public void setAdmissionFeeIsFree(String tourId) {
            this.admissionFeeIsFree = tourId;
        }

        public String getAdmissionFeeDetails() {
            return admissionFeeDetails;
        }
        public void setAdmissionFeeDetails(String tourId) {
            this.admissionFeeDetails = tourId;
        }

        public String getWebsiteUrl() {
            return websiteUrl;
        }
        public void setWebsiteUrl(String tourId) {
            this.websiteUrl = tourId;
        }

        public String getParkingAvailable() {
            return parkingAvailable;
        }
        public void setParkingAvailable(String tourId) {
            this.parkingAvailable = tourId;
        }

        public String getParkingFeeInfo() {
            return parkingFeeInfo;
        }
        public void setParkingFeeInfo(String tourId) {
            this.parkingFeeInfo = tourId;
        }

        public String getLastUpdatedNote() {
            return lastUpdatedNote;
        }
        public void setLastUpdatedNote(String tourId) {
            this.lastUpdatedNote = tourId;
        }

    }

    private List<Recommendation4> recommendations4;

    public List<Recommendation4> getRecommendations4() {
        return recommendations4;
    }

    public void setRecommendations4(List<Recommendation4> recommendations4) {
        this.recommendations4 = recommendations4;
    }

    public static class Recommendation4 {
        private int day;
        private String time;
        private String tourId;

        public int getDay() {
            return day;
        }
        public void setDay(int day) {
            this.day = day;
        }

        public String getTime() {
            return time;
        }
        public void setTime(String time) {
            this.time = time;
        }

        public String getTourId() {
            return tourId;
        }
        public void setTourId(String tourId) {
            this.tourId = tourId;
        }
    }

    private List<TranslationItem> translations;

    public List<TranslationItem> getTranslations() {
        return translations;
    }

    public void setTranslations(List<TranslationItem> translations) {
        this.translations = translations;
    }

    public static class TranslationItem {
        private String tourId;
        private String tourNm;
        private String overview;
        private String roadAddr;
        private String detailAddr;

        public String getTourId() {
            return tourId;
        }
        public void setTourId(String tourId) {
            this.tourId = tourId;
        }

        public String getTourNm() {
            return tourNm;
        }
        public void setTourNm(String tourNm) {
            this.tourNm = tourNm;
        }

        public String getOverview() {
            return overview;
        }
        public void setOverview(String overview) {
            this.overview = overview;
        }

        public String getRoadAddr() {
            return roadAddr;
        }
        public void setRoadAddr(String roadAddr) {
            this.roadAddr = roadAddr;
        }

        public String getDetailAddr() {
            return detailAddr;
        }
        public void setDetailAddr(String detailAddr) {
            this.detailAddr = detailAddr;
        }
    }

    private List<ReviewTranslationItem> reviewTranslations;

    public List<ReviewTranslationItem> getReviewTranslations() {
        return reviewTranslations;
    }

    public void setReviewTranslations(List<ReviewTranslationItem> reviewTranslations) {
        this.reviewTranslations = reviewTranslations;
    }

    public static class ReviewTranslationItem {
        private int index;
        private String reviewTitle;
        private String reviewContent;

        public int getIndex() {
            return index;
        }
        public void setIndex(int index) {
            this.index = index;
        }

        public String getReviewTitle() {
            return reviewTitle;
        }
        public void setReviewTitle(String reviewTitle) {
            this.reviewTitle = reviewTitle;
        }

        public String getReviewContent() {
            return reviewContent;
        }
        public void setReviewContent(String reviewContent) {
            this.reviewContent = reviewContent;
        }
    }
}