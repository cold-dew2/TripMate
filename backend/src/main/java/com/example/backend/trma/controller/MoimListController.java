package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.service.MoimListService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/moimList")
public class MoimListController {
    private final MoimListService moimListService;

    public MoimListController(MoimListService moimListService) {
        this.moimListService = moimListService;
    }

    //모임 검색
    @GetMapping("/moimSearch")
    public MoimSearchResponse moimSearch(@ModelAttribute MoimSearchRequest request) {

        return moimListService.moimSearch(request);
    }

    //AI 모임 추천
    @GetMapping("/moimAiSearch")
    public MoimAiSearchResponse moimSearch(@ModelAttribute MoimAiSearchRequest request) {

        return moimListService.moimAiSearch(request);
    }

    // 모임 상세조회(기본)
    @GetMapping("/moimDetail")
    public MoimDetailResponse moimDetail(@ModelAttribute MoimDetailRequest request,
                                         Authentication authentication) {

        // authentication 객체가 null이 아니고 인증된 상태인지 확인.
        // Spring Security의 AnonymousAuthenticationFilter는 비로그인 사용자에게도
        // isAuthenticated()=true인 익명 토큰(이름 "anonymousUser")을 부여하므로 이것도
        // 걸러내야 한다(안 그러면 게스트가 실제 계정처럼 취급돼 방문 이력 등록이 실패한다).
        String userId = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            userId = authentication.getName();
        }

        return moimListService.moimDetail(request, userId);
    }

    //내 모임 목록 조회
    @GetMapping("/myMoim")
    public MyMoimResponse myMoim(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new MyMoimResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/myMoim",
                    "",
                    null
            );
        }

        String userId = authentication.getName();
        return moimListService.myMoim(userId);
    }

    //내가 가입한 모임 중 오늘 진행 중인 모임들의 오늘 일정
    @GetMapping("/myTodaySchedule")
    public MyTodayScheduleResponse myTodaySchedule(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new MyTodayScheduleResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/myTodaySchedule",
                    "",
                    null
            );
        }

        String userId = authentication.getName();
        return moimListService.myTodaySchedule(userId);
    }

    //내 모임 목록 조회
    @GetMapping("/moimCateSearch")
    public MoimCateSearchResponse moimCateSearch() {

        return moimListService.moimCateSearch();
    }

    //모임 생성
    @PostMapping("/createMoim")
    public CreateMoimResponse createMoim(@RequestBody CreateMoimRequest request,
                                         Authentication authentication){

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new CreateMoimResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/createMoim",
                    null
            );
        }

        String userId = authentication.getName();
        return moimListService.createMoim(request, userId);
    }

    //모임 신청
    @PostMapping("/{moimId}/apply")
    public ApplyMoimResponse applyMoim(@PathVariable String moimId,
                                       Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new ApplyMoimResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/" + moimId + "/apply",
                    ""
            );
        }

        String userId = authentication.getName();
        return moimListService.applyMoim(moimId, userId);
    }

    //모임 멤버 목록 조회
    @GetMapping("/{moimId}/members")
    public MoimMembersResponse moimMembers(@PathVariable String moimId) {

        return moimListService.moimMembers(moimId);
    }

    //모임 멤버 상태 변경(승인/거절)
    @PutMapping("/{moimId}/members/{targetUserId}")
    public UpdateMoimMemberResponse updateMoimMember(@PathVariable String moimId,
                                                     @PathVariable String targetUserId,
                                                     @RequestBody UpdateMoimMemberRequest request,
                                                     Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new UpdateMoimMemberResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/" + moimId + "/members/" + targetUserId,
                    ""
            );
        }

        String userId = authentication.getName();
        return moimListService.updateMoimMember(moimId, targetUserId, request, userId);
    }

    //모임 일정 수정
    @PutMapping("/{moimId}/plan")
    public UpdateMoimPlanResponse updateMoimPlan(@PathVariable String moimId,
                                                 @RequestBody UpdateMoimPlanRequest request,
                                                 Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new UpdateMoimPlanResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/" + moimId + "/plan"
            );
        }

        String userId = authentication.getName();
        return moimListService.updateMoimPlan(moimId, request, userId);
    }

    //모임 관리(신청자 목록) 조회 시 가입 신청 알림 읽음 처리
    @PutMapping("/{moimId}/applicantsRead")
    public MarkApplicantsReadResponse markApplicantsRead(@PathVariable String moimId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new MarkApplicantsReadResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/" + moimId + "/applicantsRead"
            );
        }

        return moimListService.markApplicantsRead(moimId, authentication.getName());
    }

    //모임(여행) 후기 등록
    @PostMapping("/{moimId}/review")
    public CreateMoimReviewResponse createMoimReview(@PathVariable String moimId,
                                                      @RequestBody CreateMoimReviewRequest request,
                                                      Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new CreateMoimReviewResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/" + moimId + "/review",
                    ""
            );
        }

        String userId = authentication.getName();
        return moimListService.createMoimReview(moimId, request, userId);
    }

    //모임(여행) 후기 목록 조회
    @GetMapping("/{moimId}/reviews")
    public MoimReviewsResponse moimReviews(@PathVariable String moimId,
                                           @RequestParam(required = false) String lang) {

        return moimListService.moimReviews(moimId, lang);
    }

    //모임 일정 기반 교통편 혼잡도 분석(가입된 멤버만)
    @GetMapping("/{moimId}/transportRecommend")
    public TransportRecommendResponse moimTransportRecommend(@PathVariable String moimId,
                                                              Authentication authentication) {

        String userId = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            userId = authentication.getName();
        }

        return moimListService.moimTransportRecommend(moimId, userId);
    }
}
