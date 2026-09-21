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
import com.example.backend.trma.service.MoimListService;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.service.UserService;
import com.example.backend.trma.util.AiJsonUtil;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MoimListMapper moimListMapper;
    private final MoimListService moimListService;
    private final TourListService tourListService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.rememberMeExpiration}")
    private long rememberMeExpiration;

    // 로그인 무제한 시도(브루트포스) 방어용 — 인스턴스 하나짜리 배포라 인메모리로 충분하다.
    // 아이디당 실패 횟수를 세다가 LOGIN_MAX_ATTEMPTS번 연속 실패하면 잠깐 잠근다.
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final long LOGIN_LOCK_DURATION_MILLIS = 60_000;
    private static class LoginFailureState {
        int failCount = 0;
        long lockedUntilMillis = 0;
    }
    private final Map<String, LoginFailureState> loginFailures = new java.util.concurrent.ConcurrentHashMap<>();

    private boolean isLoginLocked(String userId) {
        if (userId == null) return false;
        LoginFailureState state = loginFailures.get(userId);
        return state != null && state.lockedUntilMillis > System.currentTimeMillis();
    }

    private synchronized void recordLoginFailure(String userId) {
        if (userId == null) return;
        LoginFailureState state = loginFailures.computeIfAbsent(userId, k -> new LoginFailureState());
        state.failCount++;
        if (state.failCount >= LOGIN_MAX_ATTEMPTS) {
            state.lockedUntilMillis = System.currentTimeMillis() + LOGIN_LOCK_DURATION_MILLIS;
            state.failCount = 0;
        }
    }

    private void clearLoginFailure(String userId) {
        if (userId != null) loginFailures.remove(userId);
    }

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
            // 무제한으로 비밀번호를 계속 시도할 수 있어(브루트포스 방어가 전혀 없었음),
            // 같은 아이디로 짧은 시간 안에 실패가 반복되면 잠깐 잠근다.
            if (isLoginLocked(request.getUserId())) {
                return new LoginResponse(
                        false,
                        429,
                        "LOGIN_LOCKED",
                        "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.",
                        "/login/login",
                        null
                );
            }

            String userPw = userMapper.login(request);

            // "아이디가 없음"과 "비밀번호가 틀림"을 다른 메시지로 구분해서 보여주면
            // 존재하는 아이디를 무작위로 찾아내는 계정 열거 공격에 악용될 수 있어,
            // 두 경우 모두 같은 메시지로 응답한다.
            if (userPw == null || !passwordEncoder.matches(request.getUserPw(), userPw)) {
                recordLoginFailure(request.getUserId());
                return new LoginResponse(
                        false,
                        401,
                        "UNAUTHORIZED",
                        "아이디 또는 비밀번호가 올바르지 않습니다.",
                        "/login/login",
                        null
                );
            }
            clearLoginFailure(request.getUserId());

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
            translateWithBudget(() -> applyMyReviewTranslations(userReview, request.getLang()), "마이페이지 리뷰");

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

            UserReviewRequest reviewRequest = new UserReviewRequest();
            reviewRequest.setUserId(userId);
            List<UserReviewData> reviewList = userMapper.reviewList(reviewRequest);

            // 프로필 번역과 리뷰 번역은 서로 무관하니 동시에 실행한다.
            CompletableFuture<Void> profileTranslation = CompletableFuture.runAsync(
                    () -> applyProfileTranslation(userDetail, userId, lang));
            CompletableFuture<Void> reviewTranslation = CompletableFuture.runAsync(
                    () -> applyMyReviewTranslations(reviewList, lang));
            try {
                CompletableFuture.allOf(profileTranslation, reviewTranslation).get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("마이페이지 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. userId={}, lang={}", userId, lang);
            } catch (Exception e) {
                log.warn("마이페이지 번역 대기 중 오류가 발생했습니다. userId={}, lang={}", userId, lang, e);
            }

            List<LanguageCardData> languages = userMapper.userLanguages(userId);

            List<RecentReviewData> recentReviews = new ArrayList<>();
            for (UserReviewData review : reviewList) {
                recentReviews.add(new RecentReviewData(
                        userId + "-" + recentReviews.size(),
                        review.getUserNm(),
                        review.getProfileImgUrl(),
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

    // 캐시가 없어 Gemini를 불러야 하는 최초 조회에서도 응답이 오래 붙잡히지 않도록
    // 번역 작업마다 최대 0.5초만 기다린다. 못 끝나면 한국어로라도 바로 응답하고, 번역은
    // 백그라운드에서 계속 돌아 캐시에 저장되어 다음 조회부터는 즉시 나온다.
    private void translateWithBudget(Runnable task, String label) {
        try {
            CompletableFuture.runAsync(task).get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("{} 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다.", label);
        } catch (Exception e) {
            log.warn("{} 번역 대기 중 오류가 발생했습니다.", label, e);
        }
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

    // 마이페이지/공개 프로필의 리뷰 목록 번역. 관광지 상세에서 같은 리뷰를 볼 때 이미
    // REVIEW_CONTENT_EN/JA에 캐시된 값이 있으면 재사용하고, 없는 것만 모아 번역 후
    // 캐시에 저장한다(TB_TRMA_MOIM_REVIEW 캐시를 다른 화면과 공유).
    private void applyMyReviewTranslations(List<UserReviewData> reviews, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (reviews == null || reviews.isEmpty()) return;

        Map<String, String> toTranslate = new HashMap<>();
        for (UserReviewData review : reviews) {
            if (review.getReviewContent() == null || review.getReviewContent().isBlank()) continue;

            String cached = "en".equals(lang) ? review.getReviewContentEn() : review.getReviewContentJa();
            if (cached != null && !cached.isBlank() && !AiJsonUtil.containsHangul(cached)) {
                review.setReviewContent(cached);
            } else {
                toTranslate.put(review.getReviewId(), review.getReviewContent());
            }
        }
        if (toTranslate.isEmpty()) return;

        try {
            Map<String, String> translated = tourListService.translateFreeTexts(toTranslate, lang);
            if (translated.isEmpty()) return;

            Map<String, UserReviewData> byId = new HashMap<>();
            for (UserReviewData review : reviews) byId.put(review.getReviewId(), review);

            for (Map.Entry<String, String> entry : translated.entrySet()) {
                UserReviewData review = byId.get(entry.getKey());
                String content = entry.getValue();
                if (review == null || content == null || content.isBlank()) continue;

                moimListMapper.updateReviewTranslation(
                        entry.getKey(),
                        "en".equals(lang) ? content : null,
                        "ja".equals(lang) ? content : null
                );
                review.setReviewContent(content);
            }
        } catch (Exception e) {
            log.warn("마이페이지 리뷰 번역에 실패해 한국어로 표시합니다. lang={}", lang, e);
        }
    }

    //공개 사용자 프로필 조회
    @Override
    public PublicProfileResponse publicProfile(String targetUserId, String lang) {

        try {
            UserDetailRequest detailRequest = new UserDetailRequest();
            detailRequest.setUserId(targetUserId);
            UserDetailData userDetail = userMapper.userDetail(detailRequest);

            List<LanguageCardData> languages = userMapper.userLanguages(targetUserId);

            UserReviewRequest reviewRequest = new UserReviewRequest();
            reviewRequest.setUserId(targetUserId);
            List<UserReviewData> reviews = userMapper.reviewList(reviewRequest);

            CompletableFuture<Void> profileTranslation = CompletableFuture.runAsync(
                    () -> applyProfileTranslation(userDetail, targetUserId, lang));
            CompletableFuture<Void> reviewTranslation = CompletableFuture.runAsync(
                    () -> applyMyReviewTranslations(reviews, lang));
            try {
                CompletableFuture.allOf(profileTranslation, reviewTranslation).get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("공개 프로필 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. userId={}, lang={}", targetUserId, lang);
            } catch (Exception e) {
                log.warn("공개 프로필 번역 대기 중 오류가 발생했습니다. userId={}, lang={}", targetUserId, lang, e);
            }

            List<MyMoimData> moims = moimListMapper.myMoim(targetUserId);
            if ("en".equals(lang) || "ja".equals(lang)) {
                List<String> moimIds = moims.stream().map(MyMoimData::getMoimId).toList();
                translateWithBudget(() -> {
                    Map<String, String> titles = moimListService.translateMoimTitles(moimIds, lang);
                    for (MyMoimData moim : moims) {
                        String title = titles.get(moim.getMoimId());
                        if (title != null && !title.isBlank()) moim.setMoimTitle(title);
                    }
                }, "공개 프로필 모임 목록");
            }

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