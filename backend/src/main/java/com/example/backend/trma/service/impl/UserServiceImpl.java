package com.example.backend.trma.service.impl;

import com.example.backend.global.jwt.JwtUtil;
import com.example.backend.trma.dto.dataList.UserDetailData;
import com.example.backend.trma.dto.dataList.UserReviewData;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.UserMapper;
import com.example.backend.trma.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long expiration;

    //아이디 중복확인
    @Override
    public ExistsUserIdResponse existsUserId(ExistsUserIdRequest request) {

        try {
            int count = userMapper.existsUserId(request.getUserId());

            if (count > 0) {
                return new ExistsUserIdResponse(
                        false,
                        409,
                        "USER_ALREADY_EXISTS",
                        "이미 존재하는 아이디",
                        "/login/existsUserId",
                        null
                );
            }

            return new ExistsUserIdResponse(
                    true,
                    200,
                    "SUCCESS",
                    "사용가능한 아이디",
                    "/login/existsUserId",
                    null
            );
        } catch (Exception e) {
            return new ExistsUserIdResponse(
                    false,
                    500,
                    "FAIL",
                    "아이디 중복확인 중 오류가 발생했습니다.",
                    "/login/existsUserId",
                    null
            );
        }
    }

    //회원가입
    @Override
    public SignupResponse signup(SignupRequest request) {

        try {
            if (userMapper.existsUserId(request.getUserId()) > 0) {
                return new SignupResponse(
                        false,
                        409,
                        "USER_ALREADY_EXISTS",
                        "이미 존재하는 아이디",
                        "/login/signup",
                        null
                );
            }

            request.setUserPw(passwordEncoder.encode(request.getUserPw()));
            request.setStateCd("A");

            userMapper.insertUser(request);

            return new SignupResponse(
                    true,
                    200,
                    "SUCCESS",
                    "회원가입 성공",
                    "/login/signup",
                    null
            );
        } catch (Exception e) {
            return new SignupResponse(
                    false,
                    500,
                    "FAIL",
                    "회원가입 중 오류가 발생했습니다.",
                    "/login/signup",
                    null
            );
        }
    }

    //로그인
    @Override
    public LoginResponse login(LoginRequest request) {

        try {
            String userPw = userMapper.login(request);

            if (userPw == null) {
                return new LoginResponse(
                        false,
                        401,
                        "UNAUTHORIZED",
                        "아이디가 올바르지 않습니다.",
                        "/login/login",
                        null
                );
            }

            if (!passwordEncoder.matches(request.getUserPw(), userPw)) {
                return new LoginResponse(
                        false,
                        401,
                        "UNAUTHORIZED",
                        "아이디 또는 비밀번호가 올바르지 않습니다.",
                        "/login/login",
                        null
                );
            }

            String token = jwtUtil.createToken(
                    request.getUserId(),
                    expiration
            );

            request.setLoginToken(token);

            userMapper.insertUserHist(request);

            return new LoginResponse(
                    true,
                    200,
                    "SUCCESS",
                    "로그인 성공",
                    "/login/login",
                    token
            );
        } catch (Exception e) {
            return new LoginResponse(
                    false,
                    500,
                    "FAIL",
                    "로그인 중 오류가 발생했습니다.",
                    "/login/signup",
                    null
            );
        }
    }

    //마이페이지
    @Override
    public UserDetailResponse userDetail(UserDetailRequest request) {

        try {

            UserReviewRequest reviewRequst = new UserReviewRequest();
            reviewRequst.setUserId(request.getUserId());
            UserDetailData userDetail = userMapper.userDetail(request);
            List<UserReviewData> userReview = userMapper.reviewList(reviewRequst);

            return new UserDetailResponse(
                    true,
                    200,
                    "SUCCESS",
                    "마이페이지 조회 완료",
                    "/login/userDetail",
                    "",
                    userDetail,
                    userReview
            );
        } catch (Exception e) {
            return new UserDetailResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/login/userDetail",
                    "",
                    null,
                    null
            );
        }
    }

    //마이페이지_리뷰
    @Override
    public UserReviewResponse reviewList(UserReviewRequest request) {

        try {

            if(request.getPage() == 0){
                request.setPage(1);
            }
            int offset = (request.getPage() - 1) * 10 ;
            request.setOffset(offset);

            List<UserReviewData> userReview = userMapper.reviewList(request);

            return new UserReviewResponse(
                    true,
                    200,
                    "SUCCESS",
                    "리뷰 조회 완료",
                    "/login/b",
                    "",
                    userReview
            );
        } catch (Exception e) {
            return new UserReviewResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/login/b",
                    "",
                    null
            );
        }
    }
}