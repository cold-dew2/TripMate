package com.example.backend.trma.util;

import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// 사용자가 쓴 콘텐츠 목록(후기 등)의 한 필드를 화면 언어로 바꿔 넣는 공통 처리.
//  1) 원문이 이미 화면 언어면 그대로, 2) 그 언어 캐시가 검증을 통과하면 캐시, 3) 나머지만 모아 한 번에 번역해
//  캐시에 저장(검증은 번역기가 함)하고 표시한다. 번역이 실패하면 원문을 그대로 둔다(조회 자체는 실패하지 않음).
@Slf4j
public final class ContentTranslationHelper {

    private ContentTranslationHelper() {
    }

    public record Field<T>(Function<T, String> id,
                           Function<T, String> source,
                           Function<T, String> ko,
                           Function<T, String> en,
                           Function<T, String> ja,
                           BiConsumer<T, String> setter) {
    }

    public static <T> void apply(List<T> items, String lang, Field<T> field,
                                 BiFunction<Map<String, String>, String, Map<String, String>> translator,
                                 BiConsumer<String, String> saver) {
        if (items == null || items.isEmpty() || !ContentLangUtil.isSupported(lang)) return;
        Map<String, String> need = new LinkedHashMap<>();
        for (T item : items) {
            String source = field.source().apply(item);
            String shown = ContentLangUtil.displayFromCache(source, lang,
                    field.ko().apply(item), field.en().apply(item), field.ja().apply(item));
            if (shown != null) field.setter().accept(item, shown);
            else need.put(field.id().apply(item), source);
        }
        if (need.isEmpty()) return;

        Map<String, String> translated;
        try {
            translated = translator.apply(need, lang);
        } catch (Exception e) {
            log.warn("콘텐츠 번역에 실패해 원문으로 표시합니다. lang={}", lang, e);
            return;
        }
        for (T item : items) {
            String id = field.id().apply(item);
            String text = translated.get(id);
            if (text == null || !need.containsKey(id)) continue;
            try {
                saver.accept(id, text);
            } catch (Exception e) {
                // 캐시 저장이 실패해도 이번 응답에는 번역을 보여준다(다음 조회 때 다시 번역).
                log.warn("콘텐츠 번역 캐시 저장에 실패했습니다. id={}, lang={}", id, lang, e);
            }
            field.setter().accept(item, text);
        }
    }

    // 저장 직후 미리 번역: 필드별 원문(예: title/content)을 원문 언어 외의 ko/en/ja로 번역해 언어별로 저장한다.
    // 한 언어가 실패해도 나머지 언어는 진행하고, 실패한 언어는 조회 시 번역(apply)이 다시 채운다.
    public static void pretranslate(Map<String, String> fields,
                                    BiFunction<Map<String, String>, String, Map<String, String>> translator,
                                    BiConsumer<String, Map<String, String>> saver) {
        for (String lang : List.of(ContentLangUtil.KO, ContentLangUtil.EN, ContentLangUtil.JA)) {
            try {
                Map<String, String> need = new LinkedHashMap<>();
                fields.forEach((name, text) -> {
                    if (ContentLangUtil.needsTranslation(text, lang)) need.put(name, text);
                });
                if (need.isEmpty()) continue;
                Map<String, String> translated = translator.apply(need, lang);
                Map<String, String> valid = new LinkedHashMap<>();
                translated.forEach((name, text) -> {
                    if (need.containsKey(name) && text != null && !text.isBlank()) valid.put(name, text);
                });
                if (!valid.isEmpty()) saver.accept(lang, valid);
            } catch (Exception e) {
                log.warn("저장 후 미리 번역에 실패했습니다(조회 시 다시 번역). lang={}", lang, e);
            }
        }
    }

    // 트랜잭션 커밋이 끝난 뒤 백그라운드에서 실행한다(롤백되면 실행하지 않음, 응답을 늦추지 않음).
    public static void runAfterCommit(Runnable task) {
        Runnable async = () -> java.util.concurrent.CompletableFuture.runAsync(task);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            async.run();
                        }
                    });
        } else {
            async.run();
        }
    }
}
