package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewChatMessage {
    private long messageId;
    private String roomId;
    private String senderId;
    private String content;
}
