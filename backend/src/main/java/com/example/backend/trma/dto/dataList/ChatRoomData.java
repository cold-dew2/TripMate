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
    // 조회 시점 언어로 번역된 캐시(영어/일본어). title이 이미 화면 언어로 바뀌어 내려가므로
    // 프론트는 이 값을 직접 쓰지 않고, 다음 번역 여부 판단을 위해 서버 내부에서만 참조한다.
    private String titleEn;
    private String titleJa;
    private String lastMessage;
    private int unreadCount;
    // 이 방에서 나의 참여 상태('A' 정상 / 'K' 추방됨). 추방돼도 방 자체는 목록에
    // 남겨두고 이 값으로 구분해서 보여준다.
    private String myState;
}
