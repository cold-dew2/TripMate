package com.example.backend.trma.service.impl;

import com.example.backend.global.jwt.JwtUtil;
import com.example.backend.global.jwt.WithdrawnUserRegistry;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.UserMapper;
import com.example.backend.trma.service.MoimListService;
import com.example.backend.trma.service.TourListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[인증/회원] UserService")
class UserServiceImplTest {

    @Mock UserMapper userMapper;
    @Mock MoimListMapper moimListMapper;
    @Mock MoimListService moimListService;
    @Mock TourListService tourListService;
    @Mock JwtUtil jwtUtil;
    @Mock WithdrawnUserRegistry withdrawnUserRegistry;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userMapper, moimListMapper, moimListService, tourListService, passwordEncoder, jwtUtil, withdrawnUserRegistry);
        ReflectionTestUtils.setField(service, "expiration", 1_000L);
        ReflectionTestUtils.setField(service, "rememberMeExpiration", 9_000L);
    }

    private LoginRequest loginRequest(String id, String pw, boolean rememberMe) {
        LoginRequest request = new LoginRequest();
        request.setUserId(id);
        request.setUserPw(pw);
        request.setRememberMe(rememberMe);
        return request;
    }

    // ── 회원가입 ──

    @Test
    @DisplayName("회원가입: 비밀번호를 암호화하고 활성(A) 상태로 저장한다")
    void signup_success() {
        SignupRequest request = new SignupRequest();
        request.setUserId("new@tripmate.com");
        request.setUserPw("plain-pw");
        when(userMapper.existsUserId("new@tripmate.com")).thenReturn(0);

        SignupResponse response = service.signup(request);

        assertThat(response.isSuccess()).isTrue();
        verify(userMapper).insertUser(argThat(saved ->
                !saved.getUserPw().equals("plain-pw")
                        && passwordEncoder.matches("plain-pw", saved.getUserPw())
                        && "A".equals(saved.getStateCd())));
    }

    @Test
    @DisplayName("회원가입: 이미 있는 아이디면 409로 거절하고 저장하지 않는다")
    void signup_duplicate() {
        SignupRequest request = new SignupRequest();
        request.setUserId("dup@tripmate.com");
        when(userMapper.existsUserId("dup@tripmate.com")).thenReturn(1);

        SignupResponse response = service.signup(request);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getCode()).isEqualTo("USER_ALREADY_EXISTS");
        verify(userMapper, never()).insertUser(any());
    }

    @Test
    @DisplayName("아이디 중복확인: 사용 중이면 409, 아니면 200")
    void existsUserId() {
        ExistsUserIdRequest request = new ExistsUserIdRequest();
        request.setUserId("a@b.com");
        when(userMapper.existsUserId("a@b.com")).thenReturn(1, 0);

        assertThat(service.existsUserId(request).getStatus()).isEqualTo(409);
        assertThat(service.existsUserId(request).getStatus()).isEqualTo(200);
    }

    // ── 로그인 ──

    @Test
    @DisplayName("로그인 성공: 토큰을 발급하고 로그인 이력을 남긴다")
    void login_success() {
        when(userMapper.login(any())).thenReturn(passwordEncoder.encode("pw"));
        when(jwtUtil.createToken("u@t.com", 1_000L)).thenReturn("jwt-token");

        LoginResponse response = service.login(loginRequest("u@t.com", "pw", false));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(userMapper).insertUserHist(argThat(r -> "jwt-token".equals(r.getLoginToken())));
    }

    @Test
    @DisplayName("로그인 유지 체크 시 긴 만료시간으로 토큰을 발급한다")
    void login_rememberMe() {
        when(userMapper.login(any())).thenReturn(passwordEncoder.encode("pw"));
        when(jwtUtil.createToken("u@t.com", 9_000L)).thenReturn("long-token");

        assertThat(service.login(loginRequest("u@t.com", "pw", true)).getToken()).isEqualTo("long-token");
    }

    @Test
    @DisplayName("로그인 실패: 없는 아이디와 틀린 비밀번호는 같은 401 메시지로 응답한다(계정 열거 방지)")
    void login_failSameMessage() {
        when(userMapper.login(argThat(r -> r != null && "none@t.com".equals(r.getUserId())))).thenReturn(null);
        when(userMapper.login(argThat(r -> r != null && "u@t.com".equals(r.getUserId())))).thenReturn(passwordEncoder.encode("pw"));

        LoginResponse noUser = service.login(loginRequest("none@t.com", "pw", false));
        LoginResponse wrongPw = service.login(loginRequest("u@t.com", "wrong", false));

        assertThat(noUser.getStatus()).isEqualTo(401);
        assertThat(wrongPw.getStatus()).isEqualTo(401);
        assertThat(noUser.getMessage()).isEqualTo(wrongPw.getMessage());
        verify(jwtUtil, never()).createToken(any(), anyLong());
    }

    @Test
    @DisplayName("로그인 5회 연속 실패 시 잠기고, 맞는 비밀번호도 429로 거절한다")
    void login_lockAfterFiveFailures() {
        when(userMapper.login(any())).thenReturn(passwordEncoder.encode("pw"));

        for (int i = 0; i < 5; i++) {
            assertThat(service.login(loginRequest("u@t.com", "wrong", false)).getStatus()).isEqualTo(401);
        }
        LoginResponse locked = service.login(loginRequest("u@t.com", "pw", false));

        assertThat(locked.getStatus()).isEqualTo(429);
        assertThat(locked.getCode()).isEqualTo("LOGIN_LOCKED");
        // 다른 아이디는 영향받지 않는다.
        when(jwtUtil.createToken(any(), anyLong())).thenReturn("t");
        assertThat(service.login(loginRequest("other@t.com", "pw", false)).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 횟수가 초기화된다")
    void login_successResetsFailures() {
        when(userMapper.login(any())).thenReturn(passwordEncoder.encode("pw"));
        when(jwtUtil.createToken(any(), anyLong())).thenReturn("t");

        for (int i = 0; i < 4; i++) service.login(loginRequest("u@t.com", "wrong", false));
        assertThat(service.login(loginRequest("u@t.com", "pw", false)).isSuccess()).isTrue();
        for (int i = 0; i < 4; i++) service.login(loginRequest("u@t.com", "wrong", false));

        assertThat(service.login(loginRequest("u@t.com", "pw", false)).isSuccess()).isTrue();
    }

    // ── 아이디 찾기 / 비밀번호 재설정 ──

    @Test
    @DisplayName("아이디 찾기: 이메일 로컬파트 앞 두 글자만 보이게 마스킹한다")
    void findId_masks() {
        FindIdRequest request = new FindIdRequest();
        request.setUserNm("연찬민");
        when(userMapper.findIdByUserNm("연찬민")).thenReturn(List.of("eddy@itnr.co.kr", "a@b.com"));

        FindIdResponse response = service.findId(request);

        assertThat(response.getData()).containsExactly("ed**@itnr.co.kr", "a*@b.com");
    }

    @Test
    @DisplayName("아이디 찾기: 일치하는 회원이 없으면 404")
    void findId_notFound() {
        FindIdRequest request = new FindIdRequest();
        request.setUserNm("없는사람");
        when(userMapper.findIdByUserNm("없는사람")).thenReturn(List.of());

        assertThat(service.findId(request).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("비밀번호 재설정: 본인확인을 서버에서 다시 하고, 실패하면 변경하지 않는다")
    void resetPassword_reverifies() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUserId("u@t.com");
        request.setUserNm("홍길동");
        request.setNewUserPw("new-pw");
        when(userMapper.existsUserForReset("u@t.com", "홍길동")).thenReturn(0, 1);

        assertThat(service.resetPassword(request).getStatus()).isEqualTo(404);
        verify(userMapper, never()).updatePassword(any(), any());

        assertThat(service.resetPassword(request).isSuccess()).isTrue();
        verify(userMapper).updatePassword(eq("u@t.com"), argThat(hash -> passwordEncoder.matches("new-pw", hash)));
    }

    // ── 회원 탈퇴 ──

    private WithdrawRequest withdrawRequest(String pw) {
        WithdrawRequest request = new WithdrawRequest();
        request.setUserPw(pw);
        return request;
    }

    private void savedPassword(String id, String pw) {
        when(userMapper.login(argThat(r -> r != null && id.equals(r.getUserId())))).thenReturn(passwordEncoder.encode(pw));
    }

    @Test
    @DisplayName("탈퇴: 비밀번호가 틀리면 401로 거절하고 아무것도 지우지 않는다")
    void withdraw_wrongPassword() {
        savedPassword("u@t.com", "right-pw");

        WithdrawResponse response = service.withdraw("u@t.com", withdrawRequest("wrong-pw"));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("WRONG_PASSWORD");
        verify(userMapper, never()).anonymizeWithdrawnUser(any(), any());
        verify(userMapper, never()).deleteAllChatMemberships(any());
        verifyNoInteractions(withdrawnUserRegistry);
    }

    @Test
    @DisplayName("탈퇴: 다른 승인 멤버가 있는 진행 중 모임을 운영 중이면 409와 모임 제목을 돌려주고 막는다")
    void withdraw_blockedByActiveMoim() {
        savedPassword("u@t.com", "pw-12345");
        when(userMapper.withdrawBlockingMoims("u@t.com")).thenReturn(List.of("제주 여행"));

        WithdrawResponse response = service.withdraw("u@t.com", withdrawRequest("pw-12345"));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("OWNS_ACTIVE_MOIM");
        assertThat(response.getBlockingMoims()).containsExactly("제주 여행");
        verify(moimListService, never()).deleteMoim(any(), any());
        verify(userMapper, never()).anonymizeWithdrawnUser(any(), any());
    }

    @Test
    @DisplayName("탈퇴: 혼자인 진행 중 모임을 지우고 참여·채팅·알림을 정리한 뒤 알 수 없는 비밀번호로 익명화하고 토큰을 막는다")
    void withdraw_success() {
        savedPassword("u@t.com", "pw-12345");
        when(userMapper.withdrawBlockingMoims("u@t.com")).thenReturn(List.of());
        when(userMapper.ownedOpenMoimIds("u@t.com")).thenReturn(List.of("M1"));
        when(moimListService.deleteMoim("M1", "u@t.com"))
                .thenReturn(new DeleteMoimResponse(true, 200, "SUCCESS", "", "", ""));
        when(userMapper.anonymizeWithdrawnUser(eq("u@t.com"), any())).thenReturn(1);

        WithdrawResponse response = service.withdraw("u@t.com", withdrawRequest("pw-12345"));

        assertThat(response.isSuccess()).isTrue();
        verify(userMapper).deleteOpenMoimMemberships("u@t.com");
        verify(userMapper).deleteAllChatMemberships("u@t.com");
        verify(userMapper).deleteNotifications("u@t.com");
        verify(userMapper).deleteUserLanguages("u@t.com");
        verify(userMapper).anonymizeWithdrawnUser(eq("u@t.com"),
                argThat(hash -> !passwordEncoder.matches("pw-12345", hash)));
        verify(withdrawnUserRegistry).add("u@t.com");
    }

    @Test
    @DisplayName("탈퇴: 모임 정리가 실패하면 예외로 중단해(트랜잭션 롤백) 계정을 익명화하지 않는다")
    void withdraw_rollsBackWhenMoimCleanupFails() {
        savedPassword("u@t.com", "pw-12345");
        when(userMapper.withdrawBlockingMoims("u@t.com")).thenReturn(List.of());
        when(userMapper.ownedOpenMoimIds("u@t.com")).thenReturn(List.of("M1"));
        when(moimListService.deleteMoim("M1", "u@t.com"))
                .thenReturn(new DeleteMoimResponse(false, 500, "FAIL", "", "", ""));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.withdraw("u@t.com", withdrawRequest("pw-12345")))
                .isInstanceOf(IllegalStateException.class);
        verify(userMapper, never()).anonymizeWithdrawnUser(any(), any());
        verifyNoInteractions(withdrawnUserRegistry);
    }
}
