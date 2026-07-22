// 프론트 요청을 받아 Service에게 전달하는 역할

// 1. package
package com.example.backend.trma.controller;

// 2. import

import com.example.backend.trma.dto.request.ExistsUserIdRequest;
import com.example.backend.trma.dto.request.LoginRequest;
import com.example.backend.trma.dto.request.SignupRequest;
import com.example.backend.trma.dto.response.ExistsUserIdResponse;
import com.example.backend.trma.dto.response.LoginResponse;
import com.example.backend.trma.dto.response.SignupResponse;
import com.example.backend.trma.service.UserService;
import org.springframework.web.bind.annotation.*;

// 3. 클래스 어노테이션
@RestController
@RequestMapping("/login")
//   @RequiredArgsConstructor

// 4. 클래스 선언
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //아이디 중복확인
    @PostMapping("/existsUserId")
    public ExistsUserIdResponse existsUserId(@RequestBody ExistsUserIdRequest request){
        //System.out.println("request : " + request);
        return userService.existsUserId(request);
    }

    //회원가입
    @PostMapping("/signup")
    public SignupResponse signup(@RequestBody SignupRequest request){
        //System.out.println("request : " + request);
        return userService.signup(request);
    }

    //로그인
    @GetMapping("/login")
    public LoginResponse login(@ModelAttribute LoginRequest request){
        //System.out.println("request : " + request);
        return userService.login(request);
    }
}