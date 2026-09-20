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

    // 한국관광공사 공식 영/일 데이터에 이름은 있지만 주소 등 일부 필드가 비어 있으면
    // SQL COALESCE가 조용히 한국어 원문으로 되돌아간다. NATIVE_MATCH_YN만 보고
    // "이미 번역됐다"고 판단하면 이런 값은 영원히 한국어로 남으므로, 실제로 한글이
    // 섞여 있는지 직접 확인해 필요하면 Gemini 폴백을 타게 한다.
    public static boolean containsHangul(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x1100 && c <= 0x11FF) || (c >= 0x3130 && c <= 0x318F)) {
                return true;
            }
        }
        return false;
    }
}
