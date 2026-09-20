package com.example.backend.trma.controller;

import com.example.backend.trma.dto.response.PublicProfileResponse;
import com.example.backend.trma.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class PublicProfileController {
    private final UserService userService;

    public PublicProfileController(UserService userService) {
        this.userService = userService;
    }

    //공개 사용자 프로필 조회
    @GetMapping("/{userId}/profile")
    public PublicProfileResponse publicProfile(@PathVariable String userId, @RequestParam(required = false) String lang) {

        return userService.publicProfile(userId, lang);
    }
}
