package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class SendChatMessageRequest {
    private String content;
    private String title;
}
