package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class ResetPasswordRequest {

    private String userId;
    private String userNm;
    private String newUserPw;
}
