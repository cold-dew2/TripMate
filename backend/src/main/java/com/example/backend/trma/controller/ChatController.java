package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.SendChatMessageRequest;
import com.example.backend.trma.dto.response.ChatMessagesResponse;
import com.example.backend.trma.dto.response.ChatRoomsResponse;
import com.example.backend.trma.dto.response.LeaveChatRoomResponse;
import com.example.backend.trma.dto.response.MarkChatReadResponse;
import com.example.backend.trma.dto.response.SendChatMessageResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;
import com.example.backend.trma.service.ChatService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    //내 채팅방 목록 조회
    @GetMapping("/rooms")
    public ChatRoomsResponse chatRooms(@RequestParam(required = false) String lang, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new ChatRoomsResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/chat/rooms", "", null);
        }

        return chatService.chatRooms(authentication.getName(), lang);
    }

    //채팅 메시지 목록 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ChatMessagesResponse chatMessages(@PathVariable String roomId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new ChatMessagesResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/chat/rooms/" + roomId + "/messages", "", null, 0, null);
        }

        return chatService.chatMessages(roomId, authentication.getName());
    }

    //채팅 메시지 전송
    @PostMapping("/rooms/{roomId}/messages")
    public SendChatMessageResponse sendMessage(@PathVariable String roomId,
                                               @RequestBody SendChatMessageRequest request,
                                               Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new SendChatMessageResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/chat/rooms/" + roomId + "/messages", "", null);
        }

        return chatService.sendMessage(roomId, request, authentication.getName());
    }

    //전체 안읽은 메시지 수 조회
    @GetMapping("/unreadCount")
    public UnreadCountResponse unreadCount(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new UnreadCountResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/chat/unreadCount", "", 0);
        }

        return chatService.unreadTotal(authentication.getName());
    }

    //채팅방 나가기
    @DeleteMapping("/rooms/{roomId}")
    public LeaveChatRoomResponse leaveChatRoom(@PathVariable String roomId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new LeaveChatRoomResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/chat/rooms/" + roomId);
        }

        return chatService.leaveChatRoom(roomId, authentication.getName());
    }

    //채팅방 읽음 처리(전체 메시지 재조회 없이 가볍게)
    @PutMapping("/rooms/{roomId}/read")
    public MarkChatReadResponse markRead(@PathVariable String roomId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new MarkChatReadResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/chat/rooms/" + roomId + "/read", null);
        }

        return chatService.markRead(roomId, authentication.getName());
    }
}
