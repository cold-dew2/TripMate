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

@Mapper
public interface UserMapper {
    //아이디 중복 확인
    int existsUserId(String userId);

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

    //사용 언어 삭제(전체 교체용)
    int deleteUserLanguages(String userId);

    //사용 언어 등록
    int insertUserLanguage(@Param("userId") String userId,
                           @Param("langCd") String langCd,
                           @Param("levelCd") String levelCd,
                           @Param("sortOrder") int sortOrder);

    //사용 언어 목록 조회
    List<LanguageCardData> userLanguages(String userId);

}
