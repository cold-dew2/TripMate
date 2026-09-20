package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.UpdateProfileRequest;
import com.example.backend.trma.dto.response.MyProfileResponse;
import com.example.backend.trma.dto.response.UpdateProfileResponse;
import com.example.backend.trma.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mypage")
public class MyPageController {
    private final UserService userService;

    public MyPageController(UserService userService) {
        this.userService = userService;
    }

    //마이페이지_프로필 조회
    @GetMapping("/profile")
    public MyProfileResponse profile(@RequestParam(required = false) String lang, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new MyProfileResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/mypage/profile",
                    "",
                    null
            );
        }

        String userId = authentication.getName();
        return userService.myProfile(userId, lang);
    }

    //마이페이지_프로필 수정
    @PutMapping("/profile")
    public UpdateProfileResponse updateProfile(@RequestBody UpdateProfileRequest request,
                                               Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new UpdateProfileResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/mypage/profile",
                    "",
                    null
            );
        }

        String userId = authentication.getName();
        return userService.updateProfile(request, userId);
    }
}
