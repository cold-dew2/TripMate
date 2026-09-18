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
}
