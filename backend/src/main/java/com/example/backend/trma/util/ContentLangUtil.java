package com.example.backend.trma.util;

// 사용자가 직접 쓴 콘텐츠(모임 제목/소개, 후기, 프로필 소개, 채팅)의 원문 언어 판별과 번역 결과 검증.
// 원문 언어를 별도 컬럼에 저장하지 않고 글자 자체로 판별한다(기존 데이터도 그대로 판별되고, 한국어/영어/일본어는
// 쓰는 문자가 뚜렷이 달라 문장 단위로는 안정적으로 구분된다).
public final class ContentLangUtil {

    private ContentLangUtil() {
    }

    public static final String KO = "ko";
    public static final String EN = "en";
    public static final String JA = "ja";

    // 한글 1자는 로마자 여러 자만큼의 정보라, 한국어 문장에 섞인 영어 단어나 영어 문장에 섞인 한국어 지명
    // 때문에 잘못 판별되지 않도록 문자 수에 가중치를 둔다. 글자가 없으면(숫자/이모지만) null.
    public static String detect(String text) {
        if (text == null || text.isBlank()) return null;
        int hangul = 0, kana = 0, kanji = 0, latin = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x1100 && c <= 0x11FF) || (c >= 0x3130 && c <= 0x318F)) hangul++;
            else if (c >= 0x3040 && c <= 0x30FF) kana++;
            else if (c >= 0x4E00 && c <= 0x9FFF) kanji++;
            else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) latin++;
        }
        int ko = hangul * 3;
        // 한자는 가나와 함께 쓰이거나 한글/로마자가 전혀 없을 때만 일본어 근거로 본다(한국어 문장 속 한자 표기 대비).
        int ja = kana * 3 + ((kana > 0 || (hangul == 0 && latin == 0)) ? kanji * 3 : 0);
        int en = latin;
        if (ko == 0 && ja == 0 && en == 0) return null;
        if (ko >= ja && ko >= en) return KO;
        if (ja >= en) return JA;
        return EN;
    }

    // 원문 언어를 모를 때(글자가 없음)는 번역할 필요가 없는 것으로 본다(숫자·이모지는 그대로 보여도 된다).
    public static boolean needsTranslation(String text, String targetLang) {
        String src = detect(text);
        return src != null && isSupported(targetLang) && !src.equals(targetLang);
    }

    public static boolean isSupported(String lang) {
        return KO.equals(lang) || EN.equals(lang) || JA.equals(lang);
    }

    // 콘텐츠 한 항목의 화면 표시값. 원문이 이미 그 언어면 원문, 아니면 그 언어 캐시 중 검증을 통과한 값,
    // 둘 다 아니면 null(번역 필요). 캐시에 원문/다른 언어가 잘못 저장돼 있어도 쓰지 않는다.
    public static String displayFromCache(String source, String lang, String ko, String en, String ja) {
        if (source == null || source.isBlank() || !needsTranslation(source, lang)) return source;
        String cached = KO.equals(lang) ? ko : EN.equals(lang) ? en : ja;
        return isValidTranslation(source, cached, lang) ? cached : null;
    }

    // AI 번역 결과를 저장하기 전에 확인한다: 비어 있지 않고, 대상 언어의 문자로 쓰여 있어야 한다
    // (번역에 실패해 원문을 그대로 돌려주거나 엉뚱한 언어로 답한 결과가 캐시에 굳지 않게).
    public static boolean isValidTranslation(String source, String translated, String targetLang) {
        if (translated == null || translated.isBlank()) return false;
        String detected = detect(translated);
        if (detected == null) return source != null && detect(source) == null;
        return detected.equals(targetLang);
    }
}
