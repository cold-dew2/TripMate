// 프론트 요청을 받아 Service에게 전달하는 역할

// 1. package
package com.example.backend.trma.controller;

// 2. import

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.service.UserService;
import org.springframework.security.core.Authentication;
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
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request){
        //System.out.println("request : " + request);
        return userService.login(request);
    }

    //마이페이지
    @GetMapping("/userDetail")
    public UserDetailResponse userDetail(@ModelAttribute UserDetailRequest request){
        //System.out.println("request : " + request);
        return userService.userDetail(request);
    }

    //마이페이지_리뷰조회
    @GetMapping("/reviewList")
    public UserReviewResponse reviewList(@ModelAttribute UserReviewRequest request, Authentication authentication){

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new UserReviewResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/login/reviewList",
                    "",
                    null
            );
        }

        request.setUserId(authentication.getName());
        return userService.reviewList(request);
    }
}