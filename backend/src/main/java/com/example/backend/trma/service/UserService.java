package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;

public interface UserService {

    //아이디 중복확인
    ExistsUserIdResponse existsUserId(ExistsUserIdRequest request);

    //회원가입
    SignupResponse signup(SignupRequest request);

    //로그인
    LoginResponse login(LoginRequest request);

    //마이페이지
    UserDetailResponse userDetail(UserDetailRequest request);

    //마이페이지
    UserReviewResponse reviewList(UserReviewRequest request);

}