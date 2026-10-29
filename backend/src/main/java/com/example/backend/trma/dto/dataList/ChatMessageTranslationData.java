package com.example.backend.trma.dto.dataList;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 메시지 한 건의 요청 시 번역 결과. state:
//  SOURCE(원문이 이미 그 언어), CACHED(저장된 번역), TRANSLATED(이번에 번역해 저장), FAILED(AI 실패, 원문 유지)
@Getter
@AllArgsConstructor
public class ChatMessageTranslationData {
    private String messageId;
    private String lang;
    private String content;
    private String state;
}
