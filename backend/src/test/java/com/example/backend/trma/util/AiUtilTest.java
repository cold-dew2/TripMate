package com.example.backend.trma.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchException;

@DisplayName("[공통] AI 유틸")
class AiUtilTest {

    @Test
    @DisplayName("코드펜스(```json)로 감싼 Gemini 응답에서 JSON만 추출한다")
    void extractJson_stripsCodeFence() {
        assertThat(AiJsonUtil.extractJson("```json\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
        assertThat(AiJsonUtil.extractJson("  {\"a\":1}  ")).isEqualTo("{\"a\":1}");
        assertThat(AiJsonUtil.extractJson(null)).isEmpty();
    }

    @Test
    @DisplayName("한글 포함 여부를 판별한다")
    void containsHangul() {
        assertThat(AiJsonUtil.containsHangul("서울 종로구")).isTrue();
        assertThat(AiJsonUtil.containsHangul("ㅅㅊ")).isTrue();
        assertThat(AiJsonUtil.containsHangul("Jongno-gu, Seoul")).isFalse();
        assertThat(AiJsonUtil.containsHangul(null)).isFalse();
    }

    @Test
    @DisplayName("번역 결과가 비었거나 한글이 남아 있으면 번역 실패로 본다")
    void isTranslated() {
        assertThat(AiJsonUtil.isTranslated("Gyeongbokgung Palace")).isTrue();
        assertThat(AiJsonUtil.isTranslated("景福宮")).isTrue();
        assertThat(AiJsonUtil.isTranslated("경복궁")).isFalse();
        assertThat(AiJsonUtil.isTranslated("  ")).isFalse();
        assertThat(AiJsonUtil.isTranslated(null)).isFalse();
    }

    @Test
    @DisplayName("Gemini 429/503만 'AI 일시 불가'로 분류한다")
    void isAiOverloaded() {
        assertThat(AiErrorUtil.isAiOverloaded(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "", null, null, null))).isTrue();
        assertThat(AiErrorUtil.isAiOverloaded(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "", null, null, null))).isTrue();
        assertThat(AiErrorUtil.isAiOverloaded(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "", null, null, null))).isFalse();
        assertThat(AiErrorUtil.isAiOverloaded(new ResourceAccessException("timeout"))).isFalse();
    }

    @Test
    @DisplayName("Gemini 요청의 시간 초과/연결 실패도 'AI 일시 불가'로 분류하고, 다른 외부 API의 I/O 오류는 제외한다")
    void isAiOverloaded_geminiTimeout() {
        assertThat(AiErrorUtil.isAiOverloaded(new ResourceAccessException(
                "I/O error on POST request for \"https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent\": Request cancelled")))
                .isTrue();
        assertThat(AiErrorUtil.isAiOverloaded(new ResourceAccessException(
                "I/O error on GET request for \"https://apis.data.go.kr/B551011/KorService2/detailCommon2\": Read timed out")))
                .isFalse();
        // 시간 초과는 안내는 'AI 불가'로 하되 재시도는 하지 않는다(재시도하면 같은 시간만큼 또 기다림).
        assertThat(AiErrorUtil.isRetryable(new ResourceAccessException(
                "I/O error on POST request for \"https://generativelanguage.googleapis.com/x\": Request cancelled"))).isFalse();
        assertThat(AiErrorUtil.isRetryable(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "", null, null, null))).isTrue();
        // 시간 초과 뒤 1분은 호출을 쉬어, 다음 요청이 또 15초를 기다리지 않게 한다(다른 API 시간 초과는 해당 없음).
        assertThat(AiErrorUtil.quotaCooldownMillis(new ResourceAccessException(
                "I/O error on POST request for \"https://generativelanguage.googleapis.com/x\": Request cancelled"))).isEqualTo(60_000L);
        assertThat(AiErrorUtil.quotaCooldownMillis(new ResourceAccessException("Read timed out"))).isZero();
    }

    @AfterEach
    void clearBlock() {
        AiErrorUtil.clearQuotaBlock();
    }

    static HttpClientErrorException tooMany(String body) {
        return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "", HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("429 중 일일 한도는 10분, 긴 retryDelay(분당 한도)는 그 시간만큼 쉬고, 짧은 제한/503은 재시도 대상(0)")
    void quotaCooldownMillis() {
        assertThat(AiErrorUtil.quotaCooldownMillis(tooMany(
                "{\"quotaId\":\"GenerateRequestsPerDayPerProjectPerModel-FreeTier\"}"))).isEqualTo(10 * 60_000L);
        assertThat(AiErrorUtil.quotaCooldownMillis(tooMany(
                "{\"quotaId\":\"GenerateRequestsPerMinutePerProjectPerModel-FreeTier\"},{\"retryDelay\": \"57s\"}"))).isEqualTo(57_000L);
        assertThat(AiErrorUtil.quotaCooldownMillis(tooMany(
                "{\"quotaId\":\"GenerateRequestsPerMinutePerProjectPerModel-FreeTier\"},{\"retryDelay\":\"2s\"}"))).isZero();
        assertThat(AiErrorUtil.quotaCooldownMillis(tooMany("{\"error\":{\"code\":429}}"))).isZero();
        assertThat(AiErrorUtil.quotaCooldownMillis(
                HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "", HttpHeaders.EMPTY, null, null))).isZero();
    }

    @Test
    @DisplayName("결제 한도 초과 429(exceeded your current quota)와 402(크레딧 소진)도 재시도하지 않고 10분 쉰다")
    void billingQuotaCooldown() {
        assertThat(AiErrorUtil.quotaCooldownMillis(tooMany(
                "{\"error\":{\"code\":429,\"message\":\"You exceeded your current quota, please check your plan and billing details.\"}}")))
                .isEqualTo(10 * 60_000L);
        var paymentRequired = org.springframework.web.client.HttpClientErrorException.create(
                HttpStatus.PAYMENT_REQUIRED, "", HttpHeaders.EMPTY, "{\"error\":{\"code\":402}}".getBytes(), null);
        assertThat(AiErrorUtil.quotaCooldownMillis(paymentRequired)).isEqualTo(10 * 60_000L);
        assertThat(AiErrorUtil.isAiOverloaded(paymentRequired)).isTrue();
    }

    @Test
    @DisplayName("한도 초과 표시 후에는 호출 전에 바로 429를 던지고, 기존 'AI 일시 불가' 판별에도 걸린다")
    void quotaBlock() {
        assertThatCode(AiErrorUtil::throwIfQuotaBlocked).doesNotThrowAnyException();

        AiErrorUtil.markQuotaBlocked(60_000L);
        Exception blocked = catchException(AiErrorUtil::throwIfQuotaBlocked);

        assertThat(blocked).isInstanceOf(HttpClientErrorException.class);
        assertThat(AiErrorUtil.isAiOverloaded(blocked)).isTrue();

        AiErrorUtil.clearQuotaBlock();
        assertThatCode(AiErrorUtil::throwIfQuotaBlocked).doesNotThrowAnyException();
    }
}
