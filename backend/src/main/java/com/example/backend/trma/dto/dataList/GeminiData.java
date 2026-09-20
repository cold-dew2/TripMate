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

    private MoimTranslationItem moimTranslation;

    public MoimTranslationItem getMoimTranslation() {
        return moimTranslation;
    }

    public void setMoimTranslation(MoimTranslationItem moimTranslation) {
        this.moimTranslation = moimTranslation;
    }

    public static class MoimTranslationItem {
        private String moimTitle;
        private String moimDscr;

        public String getMoimTitle() {
            return moimTitle;
        }
        public void setMoimTitle(String moimTitle) {
            this.moimTitle = moimTitle;
        }

        public String getMoimDscr() {
            return moimDscr;
        }
        public void setMoimDscr(String moimDscr) {
            this.moimDscr = moimDscr;
        }
    }

    private List<TransportLegItem> transportLegs;

    public List<TransportLegItem> getTransportLegs() {
        return transportLegs;
    }

    public void setTransportLegs(List<TransportLegItem> transportLegs) {
        this.transportLegs = transportLegs;
    }

    public static class TransportLegItem {
        private int day;
        private String fromTourId;
        private String toTourId;
        private String mode;
        private Integer durationMinutes;
        private Integer cost;
        private Integer transferCount;
        private String congestionLevel;
        private Integer delayRiskMinutes;
        private String alternativeMode;
        private String alternativeReason;

        public int getDay() {
            return day;
        }
        public void setDay(int day) {
            this.day = day;
        }

        public String getFromTourId() {
            return fromTourId;
        }
        public void setFromTourId(String fromTourId) {
            this.fromTourId = fromTourId;
        }

        public String getToTourId() {
            return toTourId;
        }
        public void setToTourId(String toTourId) {
            this.toTourId = toTourId;
        }

        public String getMode() {
            return mode;
        }
        public void setMode(String mode) {
            this.mode = mode;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }
        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Integer getCost() {
            return cost;
        }
        public void setCost(Integer cost) {
            this.cost = cost;
        }

        public Integer getTransferCount() {
            return transferCount;
        }
        public void setTransferCount(Integer transferCount) {
            this.transferCount = transferCount;
        }

        public String getCongestionLevel() {
            return congestionLevel;
        }
        public void setCongestionLevel(String congestionLevel) {
            this.congestionLevel = congestionLevel;
        }

        public Integer getDelayRiskMinutes() {
            return delayRiskMinutes;
        }
        public void setDelayRiskMinutes(Integer delayRiskMinutes) {
            this.delayRiskMinutes = delayRiskMinutes;
        }

        public String getAlternativeMode() {
            return alternativeMode;
        }
        public void setAlternativeMode(String alternativeMode) {
            this.alternativeMode = alternativeMode;
        }

        public String getAlternativeReason() {
            return alternativeReason;
        }
        public void setAlternativeReason(String alternativeReason) {
            this.alternativeReason = alternativeReason;
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
        private String reviewId;
        private String reviewTitle;
        private String reviewContent;

        public String getReviewId() {
            return reviewId;
        }
        public void setReviewId(String reviewId) {
            this.reviewId = reviewId;
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

    private List<ChatRoomTranslationItem> chatRoomTranslations;

    public List<ChatRoomTranslationItem> getChatRoomTranslations() {
        return chatRoomTranslations;
    }

    public void setChatRoomTranslations(List<ChatRoomTranslationItem> chatRoomTranslations) {
        this.chatRoomTranslations = chatRoomTranslations;
    }

    public static class ChatRoomTranslationItem {
        private String roomId;
        private String title;

        public String getRoomId() {
            return roomId;
        }
        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
    }

    private ChatTranslationItem chatTranslation;

    public ChatTranslationItem getChatTranslation() {
        return chatTranslation;
    }

    public void setChatTranslation(ChatTranslationItem chatTranslation) {
        this.chatTranslation = chatTranslation;
    }

    public static class ChatTranslationItem {
        private String contentEn;
        private String contentJa;

        public String getContentEn() {
            return contentEn;
        }
        public void setContentEn(String contentEn) {
            this.contentEn = contentEn;
        }

        public String getContentJa() {
            return contentJa;
        }
        public void setContentJa(String contentJa) {
            this.contentJa = contentJa;
        }
    }
}