package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.ChatMessageData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatMessagesResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private List<ChatMessageData> data;
    // 채팅방 참여 인원 수(카카오톡처럼 "N명 참여중"을 보여주기 위함).
    private int memberCount;
    // 이 방에서 나의 참여 상태('A' 정상 / 'K' 추방됨).
    private String myState;
}
