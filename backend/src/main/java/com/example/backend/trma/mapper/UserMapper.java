package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.LanguageCardData;
import com.example.backend.trma.dto.dataList.TourSearchData;
import com.example.backend.trma.dto.dataList.UserDetailData;
import com.example.backend.trma.dto.dataList.UserReviewData;
import com.example.backend.trma.dto.request.LoginRequest;
import com.example.backend.trma.dto.request.SignupRequest;
import com.example.backend.trma.dto.request.UpdateProfileRequest;
import com.example.backend.trma.dto.request.UserDetailRequest;
import com.example.backend.trma.dto.request.UserReviewRequest;
import com.example.backend.trma.dto.response.SignupResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    //아이디 중복 확인
    int existsUserId(String userId);

    //아이디 찾기(이름으로 조회)
    java.util.List<String> findIdByUserNm(String userNm);

    //비밀번호 재설정 대상자 확인(아이디+이름 일치 여부)
    int existsUserForReset(@Param("userId") String userId, @Param("userNm") String userNm);

    //비밀번호 재설정
    int updatePassword(@Param("userId") String userId, @Param("userPw") String userPw);

    //회원가입
    int insertUser(SignupRequest request);
    SignupResponse insertUserCenter(SignupRequest request);

    //로그인
    String login(LoginRequest request);
    int insertUserHist(LoginRequest request);

    //마이페이지
    UserDetailData userDetail(UserDetailRequest request);
    //마이페이지_리뷰
    List<UserReviewData> reviewList(UserReviewRequest request);

    //마이페이지_프로필 수정
    int updateProfile(@Param("request") UpdateProfileRequest request, @Param("userId") String userId);

    //프로필(지역/소개글) 번역 캐시 저장
    int updateProfileTranslationLang(@Param("userId") String userId,
                                      @Param("lang") String lang,
                                      @Param("areaNm") String areaNm,
                                      @Param("description") String description);

    //사용 언어 삭제(전체 교체용)
    int deleteUserLanguages(String userId);

    //사용 언어 등록
    int insertUserLanguage(@Param("userId") String userId,
                           @Param("langCd") String langCd,
                           @Param("levelCd") String levelCd,
                           @Param("sortOrder") int sortOrder);

    //사용 언어 목록 조회
    List<LanguageCardData> userLanguages(String userId);

    // ===== 회원 탈퇴 =====

    //탈퇴를 막는 모임 제목: 내가 만들었고, 아직 끝나지 않았고, 나 말고 승인된 멤버가 있는 모임
    List<String> withdrawBlockingMoims(String userId);

    //탈퇴 시 정리할 내가 만든 진행 중 모임(다른 승인 멤버 없음)
    List<String> ownedOpenMoimIds(String userId);

    //진행 중인 남의 모임에서 내 참여/신청 기록 삭제(끝난 모임 기록은 후기 등을 위해 남긴다)
    int deleteOpenMoimMemberships(String userId);

    //모든 채팅방에서 나가기
    int deleteAllChatMemberships(String userId);

    //내 알림 삭제
    int deleteNotifications(String userId);

    //개인정보 비우고 탈퇴(D) 상태로 변경. 후기·메시지는 남기되 이름은 '탈퇴한 회원'으로 보인다
    int anonymizeWithdrawnUser(@Param("userId") String userId, @Param("userPw") String unusablePw);

    //탈퇴 회원 아이디 목록(토큰 차단용)
    List<String> withdrawnUserIds();

    //E2E 테스트용 계정 목록(아이디가 e2e+...@tripmate.test). 테스트 데이터 정리용, 로컬에서만 호출된다.
    List<Map<String, Object>> e2eTestAccounts();

}
