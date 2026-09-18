package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.SendChatMessageRequest;
import com.example.backend.trma.dto.response.ChatMessagesResponse;
import com.example.backend.trma.dto.response.ChatRoomsResponse;
import com.example.backend.trma.dto.response.SendChatMessageResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;

public interface ChatService {
    //내 채팅방 목록 조회
    ChatRoomsResponse chatRooms(String userId);

    //채팅 메시지 목록 조회(조회 시 자동 입장 및 읽음 처리)
    ChatMessagesResponse chatMessages(String roomId, String userId);

    //채팅 메시지 등록
    SendChatMessageResponse sendMessage(String roomId, SendChatMessageRequest request, String userId);

    //전체 안읽은 메시지 수 조회
    UnreadCountResponse unreadTotal(String userId);
}
