package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.SendChatMessageRequest;
import com.example.backend.trma.dto.response.ChatMessagesResponse;
import com.example.backend.trma.dto.response.ChatRoomsResponse;
import com.example.backend.trma.dto.response.LeaveChatRoomResponse;
import com.example.backend.trma.dto.response.MarkChatReadResponse;
import com.example.backend.trma.dto.response.SendChatMessageResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;

public interface ChatService {
    //내 채팅방 목록 조회
    ChatRoomsResponse chatRooms(String userId, String lang);

    //채팅 메시지 목록 조회(조회 시 자동 입장 및 읽음 처리)
    ChatMessagesResponse chatMessages(String roomId, String userId);

    //채팅 메시지 등록
    SendChatMessageResponse sendMessage(String roomId, SendChatMessageRequest request, String userId);

    //전체 안읽은 메시지 수 조회
    UnreadCountResponse unreadTotal(String userId);

    //채팅방 나가기(소모임 채팅방이면 모임 탈퇴까지 함께 처리)
    LeaveChatRoomResponse leaveChatRoom(String roomId, String userId);

    //채팅방 읽음 처리만 가볍게(전체 메시지 재조회 없이 안읽음 수 델타만 반환/방송)
    MarkChatReadResponse markRead(String roomId, String userId);
}
