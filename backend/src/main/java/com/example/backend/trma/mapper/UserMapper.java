package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.request.LoginRequest;
import com.example.backend.trma.dto.request.SignupRequest;
import com.example.backend.trma.dto.response.SignupResponse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    //아이디 중복 확인
    int existsUserId(String userId);

    //회원가입
    int insertUser(SignupRequest request);
    SignupResponse insertUserCenter(SignupRequest request);

    //로그인
    String login(LoginRequest request);
    int insertUserHist(LoginRequest request);
}
