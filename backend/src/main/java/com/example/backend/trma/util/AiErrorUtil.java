package com.example.backend.trma.util;

import org.springframework.web.client.RestClientResponseException;

// Gemini가 과부하(503)나 요청 제한(429)에 걸려 응답을 못 줄 때, 다른 서버 오류와 구분해서
// "AI 기능을 지금 쓸 수 없다"는 걸 사용자에게 명확히 알려주기 위한 판별 유틸.
public final class AiErrorUtil {

    public static final String CODE = "AI_UNAVAILABLE";
    public static final String MESSAGE = "AI 기능을 일시적으로 사용할 수 없어요. 잠시 후 다시 시도해주세요.";

    private AiErrorUtil() {
    }

    public static boolean isAiOverloaded(Exception e) {
        if (e instanceof RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status == 503 || status == 429;
        }
        return false;
    }
}
