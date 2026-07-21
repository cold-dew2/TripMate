package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.request.ExistsUserIdRequest;
import com.example.backend.trma.dto.request.LoginRequest;
import com.example.backend.trma.dto.request.SignupRequest;
import com.example.backend.trma.dto.response.ExistsUserIdResponse;
import com.example.backend.trma.dto.response.LoginResponse;
import com.example.backend.trma.dto.response.SignupResponse;
import com.example.backend.trma.mapper.UserMapper;
import com.example.backend.trma.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    //아이디 중복확인
    public ExistsUserIdResponse existsUserId(ExistsUserIdRequest request) {

        int count = userMapper.existsUserId(request.getUserId());

        if (count > 0) {
            return new ExistsUserIdResponse(
                    false,
                    409,
                    "USER_ALREADY_EXISTS",
                    "이미 존재하는 아이디",
                    "/login/existsUserId",
                    ""
            );
        }

        return new ExistsUserIdResponse(
                true,
                200,
                "SUCCESS",
                "사용가능한 아이디",
                "/login/existsUserId",
                ""
        );
    }

    //회원가입
    public SignupResponse signup(SignupRequest request) {

        if (userMapper.existsUserId(request.getUserId()) > 0) {
            return new SignupResponse(
                    false,
                    409,
                    "USER_ALREADY_EXISTS",
                    "이미 존재하는 아이디",
                    "/login/signup",
                    ""
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
                ""
        );
    }

    //로그인
    public LoginResponse login(LoginRequest request) {

        LoginRequest loginInfo = userMapper.login(request);

        if (loginInfo == null) {
            return new LoginResponse(
                    false,
                    401,
                    "UNAUTHORIZED",
                    "아이디 또는 비밀번호가 올바르지 않습니다.",
                    "/login/login",
                    ""
            );
        }

        if (!passwordEncoder.matches(request.getUserPw(), loginInfo.getUserPw())) {
            return new LoginResponse(
                    false,
                    401,
                    "UNAUTHORIZED",
                    "아이디 또는 비밀번호가 올바르지 않습니다.",
                    "/login/login",
                    ""
            );
        }

        String token = Jwts.builder()
                .subject(loginInfo.getUserId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(
                        Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)),
                        Jwts.SIG.HS256
                )
                .compact();

        userMapper.insertUserHist(request);

        return new LoginResponse(
                true,
                200,
                "SUCCESS",
                "로그인 성공",
                "/login/login",
                token
        );
    }
}