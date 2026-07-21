package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Setter
public class SignupRequest {

    private String userId;
    private String userPw;
    private String userNm;
    private String stateCd;
    private String roleCd;
    private LocalDate birthDt;
    private String genderCd;
    private String phoneNum;
    private LocalDateTime createdDt;
    private LocalDateTime updatedDt;
}