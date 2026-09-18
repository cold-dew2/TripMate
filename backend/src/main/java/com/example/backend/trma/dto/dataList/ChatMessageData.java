package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class ChatMessageData {
    private String messageId;
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private String createdAt;
}
