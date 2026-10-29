package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.ChatMessageTranslationData;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TranslateChatMessageResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;

    private ChatMessageTranslationData data;
}
