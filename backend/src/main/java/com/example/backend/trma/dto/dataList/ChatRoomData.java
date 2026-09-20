package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class ChatRoomData {
    private String roomId;
    private String title;
    private String lastMessage;
    private int unreadCount;
    // 이 방에서 나의 참여 상태('A' 정상 / 'K' 추방됨). 추방돼도 방 자체는 목록에
    // 남겨두고 이 값으로 구분해서 보여준다.
    private String myState;
}
