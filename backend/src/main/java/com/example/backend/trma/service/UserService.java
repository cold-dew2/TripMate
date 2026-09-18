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

    //마이페이지_프로필 조회
    MyProfileResponse myProfile(String userId);

    //마이페이지_프로필 수정
    UpdateProfileResponse updateProfile(UpdateProfileRequest request, String userId);

    //공개 사용자 프로필 조회
    PublicProfileResponse publicProfile(String targetUserId);

}