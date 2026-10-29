package com.example.backend.trma.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[공통] 사용자 콘텐츠 원문 언어 판별/번역 검증")
class ContentLangUtilTest {

    @Test
    @DisplayName("문장의 문자로 원문 언어를 판별한다(섞인 단어·지명·한자 표기에 흔들리지 않음)")
    void detect() {
        assertThat(ContentLangUtil.detect("세종 호수공원 산책해요")).isEqualTo("ko");
        assertThat(ContentLangUtil.detect("Let's walk around Sejong Lake Park")).isEqualTo("en");
        assertThat(ContentLangUtil.detect("世宗湖水公園を散歩しましょう")).isEqualTo("ja");
        assertThat(ContentLangUtil.detect("세종 Lake Park 같이 가요")).isEqualTo("ko");
        assertThat(ContentLangUtil.detect("We visited 세종 yesterday and loved it")).isEqualTo("en");
        assertThat(ContentLangUtil.detect("大韓民國 세종시 방문")).isEqualTo("ko");
        assertThat(ContentLangUtil.detect("世宗湖水公園")).isEqualTo("ja");
        assertThat(ContentLangUtil.detect("123 !!")).isNull();
        assertThat(ContentLangUtil.detect(null)).isNull();
    }

    @Test
    @DisplayName("번역 결과는 비어 있지 않고 대상 언어 문자여야 저장한다(원문 그대로·엉뚱한 언어는 거절)")
    void isValidTranslation() {
        assertThat(ContentLangUtil.isValidTranslation("Hello", "안녕하세요", "ko")).isTrue();
        assertThat(ContentLangUtil.isValidTranslation("Hello", "Hello", "ko")).isFalse();
        assertThat(ContentLangUtil.isValidTranslation("안녕", "こんにちは", "en")).isFalse();
        assertThat(ContentLangUtil.isValidTranslation("안녕", " ", "en")).isFalse();
        assertThat(ContentLangUtil.isValidTranslation("안녕", null, "ja")).isFalse();
    }

    @Test
    @DisplayName("표시값: 원문이 그 언어면 원문, 검증된 캐시면 캐시, 없거나 잘못된 캐시면 null(번역 필요)")
    void displayFromCache() {
        assertThat(ContentLangUtil.displayFromCache("세종 나들이", "ko", null, "Sejong", null)).isEqualTo("세종 나들이");
        assertThat(ContentLangUtil.displayFromCache("Sejong outing", "ko", "세종 나들이", null, null)).isEqualTo("세종 나들이");
        assertThat(ContentLangUtil.displayFromCache("Sejong outing", "ko", null, null, null)).isNull();
        // 예전에 원문이 그대로 캐시에 들어간 경우는 쓰지 않는다
        assertThat(ContentLangUtil.displayFromCache("세종 나들이", "en", null, "세종 나들이", null)).isNull();
        assertThat(ContentLangUtil.displayFromCache("Sejong outing", "en", null, null, null)).isEqualTo("Sejong outing");
        assertThat(ContentLangUtil.displayFromCache(null, "ja", null, null, null)).isNull();
    }

    static class Item {
        String id, source, ko, en, ja, shown;
        Item(String id, String source, String ko) { this.id = id; this.source = source; this.ko = ko; this.shown = source; }
    }

    static ContentTranslationHelper.Field<Item> field() {
        return new ContentTranslationHelper.Field<>(i -> i.id, i -> i.source, i -> i.ko, i -> i.en, i -> i.ja, (i, v) -> i.shown = v);
    }

    @Test
    @DisplayName("목록 번역: 캐시·원문 언어 항목은 AI 없이 표시하고, 나머지만 모아 번역 후 저장한다")
    void apply_translatesOnlyMissing() {
        Item korean = new Item("1", "좋았어요", null);
        Item cached = new Item("2", "Great tour", "멋진 투어");
        Item missing = new Item("3", "Nice view", null);
        List<Map<String, String>> asked = new ArrayList<>();
        Map<String, String> saved = new HashMap<>();

        ContentTranslationHelper.apply(List.of(korean, cached, missing), "ko", field(),
                (need, lang) -> { asked.add(need); return Map.of("3", "전망이 좋아요"); },
                saved::put);

        assertThat(asked).containsExactly(Map.of("3", "Nice view"));
        assertThat(saved).containsExactly(Map.entry("3", "전망이 좋아요"));
        assertThat(korean.shown).isEqualTo("좋았어요");
        assertThat(cached.shown).isEqualTo("멋진 투어");
        assertThat(missing.shown).isEqualTo("전망이 좋아요");
    }

    @Test
    @DisplayName("목록 번역: AI·저장이 실패해도 조회는 원문(또는 이번 번역)으로 계속된다")
    void apply_failuresFallBack() {
        Item item = new Item("1", "Nice view", null);
        ContentTranslationHelper.apply(List.of(item), "ko", field(),
                (need, lang) -> { throw new RuntimeException("ai down"); }, (id, text) -> { });
        assertThat(item.shown).isEqualTo("Nice view");

        ContentTranslationHelper.apply(List.of(item), "ko", field(),
                (need, lang) -> Map.of("1", "전망이 좋아요"), (id, text) -> { throw new RuntimeException("db down"); });
        assertThat(item.shown).isEqualTo("전망이 좋아요");
    }

    @Test
    @DisplayName("미리 번역: 원문 언어는 건너뛰고, 한 언어 실패·빈 결과는 저장하지 않으며 나머지는 진행한다")
    void pretranslate() {
        Map<String, Map<String, String>> saved = new LinkedHashMap<>();
        List<String> askedLangs = new ArrayList<>();

        ContentTranslationHelper.pretranslate(Map.of("content", "Nice view"),
                (need, lang) -> {
                    askedLangs.add(lang);
                    if ("ko".equals(lang)) throw new RuntimeException("ai down");
                    return Map.of("content", " ");
                },
                saved::put);
        assertThat(askedLangs).containsExactly("ko", "ja");
        assertThat(saved).isEmpty();

        ContentTranslationHelper.pretranslate(Map.of("content", "Nice view"),
                (need, lang) -> Map.of("content", "ko".equals(lang) ? "전망이 좋아요" : "眺めがいい"),
                saved::put);
        assertThat(saved).containsOnlyKeys("ko", "ja");
        assertThat(saved.get("ko")).containsEntry("content", "전망이 좋아요");
    }
}
