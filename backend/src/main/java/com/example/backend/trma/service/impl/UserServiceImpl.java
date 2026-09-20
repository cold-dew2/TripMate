package com.example.backend.trma.service.impl;

import com.example.backend.global.jwt.JwtUtil;
import com.example.backend.trma.dto.dataList.LanguageCardData;
import com.example.backend.trma.dto.dataList.MyMoimData;
import com.example.backend.trma.dto.dataList.MyProfileData;
import com.example.backend.trma.dto.dataList.PublicProfileData;
import com.example.backend.trma.dto.dataList.RecentReviewData;
import com.example.backend.trma.dto.dataList.UserDetailData;
import com.example.backend.trma.dto.dataList.UserReviewData;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.UserMapper;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MoimListMapper moimListMapper;
    private final TourListService tourListService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.rememberMeExpiration}")
    private long rememberMeExpiration;

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
            log.error("처리 중 오류가 발생했습니다.", e);
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

    //아이디 찾기
    @Override
    public FindIdResponse findId(FindIdRequest request) {

        try {
            List<String> userIds = userMapper.findIdByUserNm(request.getUserNm());

            if (userIds.isEmpty()) {
                return new FindIdResponse(
                        false,
                        404,
                        "NOT_FOUND",
                        "일치하는 회원 정보가 없습니다.",
                        "/login/findId",
                        null
                );
            }

            List<String> maskedIds = userIds.stream().map(this::maskUserId).toList();

            return new FindIdResponse(
                    true,
                    200,
                    "SUCCESS",
                    "아이디를 찾았습니다.",
                    "/login/findId",
                    maskedIds
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new FindIdResponse(
                    false,
                    500,
                    "FAIL",
                    "아이디 찾기 중 오류가 발생했습니다.",
                    "/login/findId",
                    null
            );
        }
    }

    // 이름으로 조회한 아이디(이메일)를 그대로 노출하지 않도록 로컬파트 앞 두 글자만 남기고 가린다.
    // (예: eddy@itnr.co.kr -> ed***@itnr.co.kr)
    private String maskUserId(String userId) {
        int at = userId.indexOf('@');
        if (at <= 0) return userId;

        String local = userId.substring(0, at);
        String domain = userId.substring(at);
        int visible = Math.min(2, local.length());
        String masked = local.substring(0, visible) + "*".repeat(Math.max(1, local.length() - visible));
        return masked + domain;
    }

    //비밀번호 재설정 대상자 확인
    @Override
    public VerifyResetResponse verifyReset(VerifyResetRequest request) {

        try {
            int count = userMapper.existsUserForReset(request.getUserId(), request.getUserNm());

            if (count == 0) {
                return new VerifyResetResponse(
                        false,
                        404,
                        "NOT_FOUND",
                        "일치하는 회원 정보가 없습니다.",
                        "/login/verifyReset"
                );
            }

            return new VerifyResetResponse(
                    true,
                    200,
                    "SUCCESS",
                    "본인 확인이 완료되었습니다.",
                    "/login/verifyReset"
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new VerifyResetResponse(
                    false,
                    500,
                    "FAIL",
                    "본인 확인 중 오류가 발생했습니다.",
                    "/login/verifyReset"
            );
        }
    }

    //비밀번호 재설정
    @Override
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {

        try {
            // 클라이언트가 검증 단계를 건너뛰고 바로 이 API를 호출할 수도 있으므로 서버에서도 다시 확인한다.
            if (userMapper.existsUserForReset(request.getUserId(), request.getUserNm()) == 0) {
                return new ResetPasswordResponse(
                        false,
                        404,
                        "NOT_FOUND",
                        "일치하는 회원 정보가 없습니다.",
                        "/login/resetPassword"
                );
            }

            userMapper.updatePassword(request.getUserId(), passwordEncoder.encode(request.getNewUserPw()));

            return new ResetPasswordResponse(
                    true,
                    200,
                    "SUCCESS",
                    "비밀번호가 변경되었습니다.",
                    "/login/resetPassword"
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new ResetPasswordResponse(
                    false,
                    500,
                    "FAIL",
                    "비밀번호 변경 중 오류가 발생했습니다.",
                    "/login/resetPassword"
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
            log.error("처리 중 오류가 발생했습니다.", e);
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
                    request.isRememberMe() ? rememberMeExpiration : expiration
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
            log.error("처리 중 오류가 발생했습니다.", e);
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
            log.error("처리 중 오류가 발생했습니다.", e);
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
            log.error("처리 중 오류가 발생했습니다.", e);
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

    //마이페이지_프로필 조회
    @Override
    public MyProfileResponse myProfile(String userId, String lang) {

        try {
            UserDetailRequest detailRequest = new UserDetailRequest();
            detailRequest.setUserId(userId);
            UserDetailData userDetail = userMapper.userDetail(detailRequest);
            applyProfileTranslation(userDetail, userId, lang);

            UserReviewRequest reviewRequest = new UserReviewRequest();
            reviewRequest.setUserId(userId);
            List<UserReviewData> reviewList = userMapper.reviewList(reviewRequest);

            List<LanguageCardData> languages = userMapper.userLanguages(userId);

            List<RecentReviewData> recentReviews = new ArrayList<>();
            for (UserReviewData review : reviewList) {
                recentReviews.add(new RecentReviewData(
                        userId + "-" + recentReviews.size(),
                        review.getUserNm(),
                        review.getReviewScore(),
                        review.getReviewContent(),
                        review.getImgUrls(),
                        review.getCreateDt()
                ));
            }

            MyProfileData profile = new MyProfileData(
                    userDetail.getUserNm(),
                    userDetail.getAreaNm(),
                    userDetail.getDescription(),
                    userDetail.getProfileImgUrl(),
                    userDetail.getRating(),
                    userDetail.getMoimRevireCnt(),
                    userDetail.getMoimCnt(),
                    userDetail.getMoimMemberCnt(),
                    languages,
                    recentReviews
            );

            return new MyProfileResponse(
                    true,
                    200,
                    "SUCCESS",
                    "마이페이지 프로필 조회를 정상적으로 조회했습니다.",
                    "/mypage/profile",
                    "",
                    profile
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MyProfileResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/mypage/profile",
                    "",
                    null
            );
        }
    }

    //마이페이지_프로필 수정
    @Override
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request, String userId) {

        try {
            userMapper.updateProfile(request, userId);

            if (request.getLanguages() != null) {
                userMapper.deleteUserLanguages(userId);
                // 언어별 구사 능력치(1~5단계)는 사용자가 직접 선택한 값을 그대로 저장한다.
                // LANG_CD가 PK의 일부라 같은 언어가 중복 들어오면 등록이 실패하므로 먼저 중복을 제거한다.
                Set<String> seenLangCds = new HashSet<>();
                List<LanguageInput> languages = request.getLanguages().stream()
                        .filter(lang -> lang.getLangCd() != null && !lang.getLangCd().isBlank())
                        .filter(lang -> seenLangCds.add(lang.getLangCd()))
                        .limit(3)
                        .toList();
                for (int i = 0; i < languages.size(); i++) {
                    LanguageInput lang = languages.get(i);
                    int level = Math.min(5, Math.max(1, lang.getLevelCd()));
                    userMapper.insertUserLanguage(userId, lang.getLangCd(), String.valueOf(level), i);
                }
            }

            return toUpdateProfileResponse(myProfile(userId, null));
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new UpdateProfileResponse(
                    false,
                    500,
                    "FAIL",
                    "수정 중 오류가 발생했습니다.",
                    "/mypage/profile",
                    "",
                    null
            );
        }
    }

    private UpdateProfileResponse toUpdateProfileResponse(MyProfileResponse profileResponse) {
        return new UpdateProfileResponse(
                profileResponse.isSuccess(),
                profileResponse.getStatus(),
                profileResponse.getCode(),
                "프로필이 수정되었습니다.",
                "/mypage/profile",
                profileResponse.getToken(),
                profileResponse.getData()
        );
    }

    // 관광지명/모임 제목과 동일한 방식(최초 조회 시 번역해 캐시)으로 지역/소개글을
    // 번역한다. 본인이 프로필을 수정하면 updateProfile에서 캐시를 지워버리므로 다음
    // 조회 때 여기서 다시 번역된다.
    private void applyProfileTranslation(UserDetailData userDetail, String userId, String lang) {
        if (userDetail == null) return;
        if (!"en".equals(lang) && !"ja".equals(lang)) return;

        String cachedArea = "en".equals(lang) ? userDetail.getAreaNmEn() : userDetail.getAreaNmJa();
        String cachedDesc = "en".equals(lang) ? userDetail.getDescriptionEn() : userDetail.getDescriptionJa();

        Map<String, String> needsTranslation = new HashMap<>();
        boolean areaCached = cachedArea != null && !cachedArea.isBlank();
        boolean descCached = cachedDesc != null && !cachedDesc.isBlank();
        if (areaCached) {
            userDetail.setAreaNm(cachedArea);
        } else if (userDetail.getAreaNm() != null && !userDetail.getAreaNm().isBlank()) {
            needsTranslation.put("areaNm", userDetail.getAreaNm());
        }
        if (descCached) {
            userDetail.setDescription(cachedDesc);
        } else if (userDetail.getDescription() != null && !userDetail.getDescription().isBlank()) {
            needsTranslation.put("description", userDetail.getDescription());
        }
        if (needsTranslation.isEmpty()) return;

        try {
            Map<String, String> translated = tourListService.translateFreeTexts(needsTranslation, lang);
            String translatedArea = translated.get("areaNm");
            String translatedDesc = translated.get("description");
            if (translatedArea == null && translatedDesc == null) return;

            userMapper.updateProfileTranslation(
                    userId,
                    "en".equals(lang) ? translatedArea : null,
                    "ja".equals(lang) ? translatedArea : null,
                    "en".equals(lang) ? translatedDesc : null,
                    "ja".equals(lang) ? translatedDesc : null
            );
            if (translatedArea != null) userDetail.setAreaNm(translatedArea);
            if (translatedDesc != null) userDetail.setDescription(translatedDesc);
        } catch (Exception e) {
            log.warn("프로필 번역에 실패해 한국어로 표시합니다. userId={}, lang={}", userId, lang, e);
        }
    }

    //공개 사용자 프로필 조회
    @Override
    public PublicProfileResponse publicProfile(String targetUserId, String lang) {

        try {
            UserDetailRequest detailRequest = new UserDetailRequest();
            detailRequest.setUserId(targetUserId);
            UserDetailData userDetail = userMapper.userDetail(detailRequest);
            applyProfileTranslation(userDetail, targetUserId, lang);

            List<LanguageCardData> languages = userMapper.userLanguages(targetUserId);

            UserReviewRequest reviewRequest = new UserReviewRequest();
            reviewRequest.setUserId(targetUserId);
            List<UserReviewData> reviews = userMapper.reviewList(reviewRequest);

            List<MyMoimData> moims = moimListMapper.myMoim(targetUserId);

            PublicProfileData profile = new PublicProfileData(
                    targetUserId,
                    userDetail.getUserNm(),
                    userDetail.getAreaNm(),
                    userDetail.getDescription(),
                    userDetail.getProfileImgUrl(),
                    userDetail.getRating(),
                    userDetail.getMoimRevireCnt(),
                    userDetail.getJoinDt(),
                    languages
            );

            return new PublicProfileResponse(
                    true,
                    200,
                    "SUCCESS",
                    "사용자 프로필을 정상적으로 조회했습니다.",
                    "/users/" + targetUserId + "/profile",
                    "",
                    profile,
                    moims,
                    reviews
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new PublicProfileResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/users/" + targetUserId + "/profile",
                    "",
                    null,
                    null,
                    null
            );
        }
    }
}