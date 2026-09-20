package com.example.backend.trma.dto.dataList;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 채팅 메시지 번역이 끝났을 때, 메시지 전체를 다시 내려주는 대신 "어느 메시지가
// 어떻게 번역됐는지"만 가볍게 실어 보내기 위한 델타 전용 DTO(ChatUnreadDeltaData와 같은 패턴).
@Getter
@AllArgsConstructor
public class ChatTranslationDelta {
    private String messageId;
    private String contentEn;
    private String contentJa;
}
