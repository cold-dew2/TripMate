// 프론트 요청을 받아 Service에게 전달하는 역할

// 1. package
package com.example.backend.trma.controller;

// 2. import

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

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

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.rememberMeExpiration}")
    private long rememberMeExpiration;

    private static final String COOKIE_NAME = "accessToken";

    //아이디 중복확인
    @PostMapping("/existsUserId")
    public ExistsUserIdResponse existsUserId(@RequestBody ExistsUserIdRequest request){
        //System.out.println("request : " + request);
        return userService.existsUserId(request);
    }

    //아이디 찾기
    @PostMapping("/findId")
    public FindIdResponse findId(@RequestBody FindIdRequest request){
        return userService.findId(request);
    }

    //비밀번호 재설정 대상자 확인
    @PostMapping("/verifyReset")
    public VerifyResetResponse verifyReset(@RequestBody VerifyResetRequest request){
        return userService.verifyReset(request);
    }

    //비밀번호 재설정
    @PostMapping("/resetPassword")
    public ResetPasswordResponse resetPassword(@RequestBody ResetPasswordRequest request){
        return userService.resetPassword(request);
    }

    //회원가입
    @PostMapping("/signup")
    public SignupResponse signup(@Valid @RequestBody SignupRequest request){
        //System.out.println("request : " + request);
        return userService.signup(request);
    }

    //로그인
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletResponse response){
        //System.out.println("request : " + request);
        LoginResponse result = userService.login(request);

        if (result.isSuccess() && result.getToken() != null) {
            long maxAgeMillis = request.isRememberMe() ? rememberMeExpiration : expiration;

            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, result.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(Duration.ofMillis(maxAgeMillis))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return result;
    }

    //로그아웃
    @PostMapping("/logout")
    public void logout(HttpServletResponse response){
        // 로그인 때 심은 쿠키(Secure=true, SameSite=None — 프론트가 다른 오리진(Vercel)에서
        // 붙는 교차 사이트 요청이라 필요했다)와 속성이 다르면, 브라우저에 따라 이 삭제용
        // Set-Cookie가 원래 쿠키를 지우지 못하고 별개로 취급될 수 있다(로그아웃해도
        // accessToken이 남아, 뒤로가기 시 로그인된 것처럼 보이는 원인이 될 수 있었다).
        // 반드시 로그인 쿠키와 동일한 속성으로 지워야 브라우저가 같은 쿠키로 인식한다.
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 로그인 여부만 가볍게 확인한다(소모임 만들기처럼 로그인한 사용자만 들어갈 수
    // 있는 화면에 접근 가드를 걸 때 쓴다). accessToken이 httpOnly 쿠키라 프론트에서는
    // 직접 로그인 여부를 알 수 없어, 실제로 인증된 요청인지 서버에 확인해야 한다.
    @GetMapping("/authCheck")
    public AuthCheckResponse authCheck(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return new AuthCheckResponse(
                    false,
                    401,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/login/authCheck",
                    ""
            );
        }
        return new AuthCheckResponse(true, 200, "SUCCESS", "로그인 상태입니다.", "/login/authCheck", "");
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