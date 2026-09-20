package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 누군가 방을 읽었을 때, 그 방의 메시지 전체를 다시 내려주는 대신 "어느 메시지의
// 안읽은 사람 수가 몇 명으로 바뀌었는지"만 가볍게 실어 보내기 위한 델타 전용 DTO.
@Getter
@NoArgsConstructor
@Setter
public class ChatUnreadDeltaData {
    private long messageId;
    private int unreadCount;
}
