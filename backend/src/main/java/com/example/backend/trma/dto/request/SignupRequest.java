package com.example.backend.trma.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Setter
public class SignupRequest {

    // 프론트는 아이디 형식/비밀번호 8자 이상을 검사하지만, API를 직접 호출하면
    // 그 검사를 건너뛸 수 있었다(빈 아이디, 1글자 비밀번호도 그대로 가입됐다).
    @NotBlank(message = "아이디를 입력해주세요.")
    private String userId;
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String userPw;
    @NotBlank(message = "이름을 입력해주세요.")
    private String userNm;
    private String langCd;
    private String stateCd;
    private String roleCd;
    private LocalDate birthDt;
    private String genderCd;
    private String phoneNum;
}