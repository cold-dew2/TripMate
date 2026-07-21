package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.request.LoginRequest;
import com.example.backend.trma.dto.request.SignupRequest;
import com.example.backend.trma.dto.response.LoginResponse;
import com.example.backend.trma.dto.response.SignupResponse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    //아이디 중복 확인
    int existsUserId(String userId);

    //회원가입
    List<Map<String, String>> insertUser(SignupRequest request);
    SignupResponse insertUserCenter(SignupRequest request);

    //로그인
    LoginRequest login(LoginRequest request);
    LoginResponse insertUserHist(LoginRequest request);
}
