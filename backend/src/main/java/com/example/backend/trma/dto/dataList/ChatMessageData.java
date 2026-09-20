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
    // 메시지 전송 시점에 비동기로 번역해서 채워두는 캐시. AI가 아직 응답하지 않았거나
    // 실패했으면 null이고, 프론트는 그 경우 content(원문)를 대신 보여준다.
    private String contentEn;
    private String contentJa;
    private String createdAt;
    // 이 메시지를 아직 안 읽은 참여자 수(발신자 본인 제외). 카카오톡처럼 0이 되면
    // 프론트에서 배지를 숨기는 방식으로 쓴다.
    private Integer unreadCount;
}
