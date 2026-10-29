package com.example.backend.global.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[인증] JWT")
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-must-be-32bytes-or-more!!";
    private final JwtUtil jwtUtil = new JwtUtil(SECRET);

    @Test
    @DisplayName("발급한 토큰에서 사용자 아이디를 꺼낼 수 있다")
    void createAndParse() {
        String token = jwtUtil.createToken("user@tripmate.com", 60_000L);
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserId(token)).isEqualTo("user@tripmate.com");
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void expiredToken() {
        String token = jwtUtil.createToken("user@tripmate.com", -1_000L);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된(위조) 토큰과 형식이 깨진 토큰은 거부한다")
    void forgedOrMalformed() {
        String forged = new JwtUtil("another-secret-key-for-unit-tests-32bytes-or-more!!!!").createToken("admin", 60_000L);
        assertThat(jwtUtil.validateToken(forged)).isFalse();
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }
}
