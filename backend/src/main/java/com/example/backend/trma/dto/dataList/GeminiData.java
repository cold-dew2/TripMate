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
}