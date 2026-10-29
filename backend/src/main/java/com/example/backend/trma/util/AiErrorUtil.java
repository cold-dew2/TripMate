package com.example.backend.trma.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Gemini가 과부하(503)나 요청 제한(429)에 걸려 응답을 못 줄 때, 다른 서버 오류와 구분해서
// "AI 기능을 지금 쓸 수 없다"는 걸 사용자에게 명확히 알려주기 위한 판별 유틸.
public final class AiErrorUtil {

    public static final String CODE = "AI_UNAVAILABLE";
    public static final String MESSAGE = "AI 기능을 일시적으로 사용할 수 없어요. 잠시 후 다시 시도해주세요.";

    // 한도를 넘긴 뒤에는 다시 불러도 계속 429만 돌아온다. 그동안은 Gemini 호출 자체를
    // 건너뛰고, 쉬는 시간이 끝나면 다시 실제로 불러 한도가 풀렸는지 확인한다.
    public static final int QUOTA_COOLDOWN_MINUTES = 10;
    static final long QUOTA_COOLDOWN_MILLIS = QUOTA_COOLDOWN_MINUTES * 60_000L;
    // 이보다 짧은 retryDelay는 기존 재시도(0.5/1/1.5초)로 충분히 넘길 수 있다고 본다.
    static final long MIN_COOLDOWN_MILLIS = 5_000L;
    static final long TIMEOUT_COOLDOWN_MILLIS = 60_000L;
    private static final Pattern RETRY_DELAY = Pattern.compile("\"retryDelay\"\\s*:\\s*\"([0-9.]+)s\"");
    private static volatile long quotaBlockedUntilMillis = 0;

    private AiErrorUtil() {
    }

    public static boolean isAiOverloaded(Exception e) {
        if (e instanceof RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status == 503 || status == 429 || status == 402;
        }
        // Gemini가 응답 없이 시간 초과/연결 실패(ResourceAccessException)로 끝나는 경우도 "AI를 지금 못 쓴다"는
        // 같은 상황이다. 예전엔 이 경우가 일반 오류로 분류돼, 사용자가 15초 기다린 뒤 "실패했어요"만 보고
        // AI 없이 진행하라는 안내를 받지 못했다. 다른 외부 API(TourAPI/카카오)의 I/O 오류는 AI 문제로
        // 오인하지 않도록 Gemini 주소로 보낸 요청만 해당시킨다.
        if (e instanceof ResourceAccessException ex) {
            return ex.getMessage() != null && ex.getMessage().contains(GEMINI_HOST);
        }
        return false;
    }

    private static final String GEMINI_HOST = "generativelanguage.googleapis.com";

    // 짧게 재시도해 볼 만한 오류인지(일시적 과부하 503/429). 시간 초과는 재시도하면 같은 시간을 또 기다리게 되므로
    // 사용자 안내용 분류(isAiOverloaded)와 달리 재시도 대상에서 뺀다.
    public static boolean isRetryable(Exception e) {
        return e instanceof RestClientResponseException && isAiOverloaded(e);
    }

    // 429는 "잠깐 기다리면 풀리는 제한"과 "당장은 안 풀리는 한도 초과"로 나뉜다. 예전에는
    // 둘을 구분하지 않고 요청마다 0.5~1.5초 간격으로 4번씩 재시도했는데, Gemini가 "57초 뒤에
    // 다시 시도하라"(분당 한도)거나 일일 한도를 다 썼다고 알려준 경우엔 그 재시도가 전부 같은
    // 429로 끝나 호출과 대기만 늘었다. 이런 경우 쉬어야 할 시간을 돌려준다(0이면 재시도해도 됨).
    //  - QuotaFailure.quotaId에 PerDay가 있으면 일일 한도 → QUOTA_COOLDOWN_MINUTES 동안
    //  - RetryInfo.retryDelay가 재시도 간격으로 감당 못 할 만큼 길면(5초 이상) → 그 시간 동안
    //  - 402(선불 크레딧 소진)는 결제 전까지 풀리지 않으므로 → QUOTA_COOLDOWN_MINUTES 동안
    public static long quotaCooldownMillis(Exception e) {
        if (e instanceof RestClientResponseException p && p.getStatusCode().value() == 402) return QUOTA_COOLDOWN_MILLIS;
        // Gemini가 응답 없이 시간 초과되면 다음 요청도 같은 시간(약 15초)을 기다리다 실패하기 쉽다.
        // 잠깐(1분) 호출을 쉬어, 그동안 들어온 요청은 기다리지 않고 바로 "AI 일시 불가"로 안내한다.
        if (e instanceof ResourceAccessException && isAiOverloaded(e)) return TIMEOUT_COOLDOWN_MILLIS;
        if (!(e instanceof RestClientResponseException ex) || ex.getStatusCode().value() != 429) return 0;
        String body = ex.getResponseBodyAsString();
        if (body == null) return 0;
        if (body.contains("PerDay")) return QUOTA_COOLDOWN_MILLIS;
        // 결제 한도/크레딧 소진("exceeded your current quota ... plan and billing")도 기다려서 풀리지 않는다.
        if (body.contains("exceeded your current quota") || body.contains("billing")) return QUOTA_COOLDOWN_MILLIS;

        Matcher retryDelay = RETRY_DELAY.matcher(body);
        if (retryDelay.find()) {
            long millis = (long) (Double.parseDouble(retryDelay.group(1)) * 1000);
            if (millis >= MIN_COOLDOWN_MILLIS) return millis;
        }
        return 0;
    }

    public static void markQuotaBlocked(long millis) {
        quotaBlockedUntilMillis = Math.max(quotaBlockedUntilMillis, System.currentTimeMillis() + millis);
    }

    // 한도 초과로 쉬는 중이면 Gemini를 부르지 않고 바로 429를 던진다. 호출부는 기존과
    // 똑같이 isAiOverloaded로 받아 "AI 일시 불가" 안내나 한국어 원문 폴백으로 처리한다.
    public static void throwIfQuotaBlocked() {
        if (System.currentTimeMillis() < quotaBlockedUntilMillis) {
            throw HttpClientErrorException.create(
                    "Gemini 사용량 한도 초과로 호출을 건너뜁니다.",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too Many Requests",
                    HttpHeaders.EMPTY,
                    "{\"skipped\":\"quota cooldown\"}".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );
        }
    }

    // 한도 초과로 쉬는 중이면 남은 시간(ms), 아니면 0. 배치 작업이 더 진행할지 판단할 때 쓴다.
    public static long quotaCooldownMillisRemaining() {
        return Math.max(0, quotaBlockedUntilMillis - System.currentTimeMillis());
    }

    // 테스트에서 다른 테스트의 차단 상태가 남지 않도록 초기화할 때 사용한다.
    public static void clearQuotaBlock() {
        quotaBlockedUntilMillis = 0;
    }
}
