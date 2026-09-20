package com.example.backend.trma.util;

// Gemini는 "JSON만 응답하라"고 프롬프트에 명시해도 종종 응답을 ```json ... ``` 코드펜스로
// 감싸서 돌려준다. 그 백틱(`)을 그대로 ObjectMapper.readValue에 넘기면 JSON 파싱에서
// "Unexpected character ('`')" 오류가 나므로, 파싱 전에 코드펜스를 벗겨내기 위한 유틸.
public final class AiJsonUtil {

    private AiJsonUtil() {
    }

    public static String extractJson(String raw) {
        if (raw == null) return "";

        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            trimmed = firstNewline >= 0 ? trimmed.substring(firstNewline + 1) : trimmed.substring(3);

            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd >= 0) {
                trimmed = trimmed.substring(0, fenceEnd);
            }
        }
        return trimmed.trim();
    }
}
