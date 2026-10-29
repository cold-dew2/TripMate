package com.example.backend.trma.dto.dataList;

import com.example.backend.trma.util.ContentLangUtil;
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
    // 사용자가 번역을 요청했을 때 채워지는 언어별 캐시(메시지마다 한 번만 번역).
    // 아직 요청되지 않았거나 실패했으면 null이고, 프론트는 원문(content)을 보여준다.
    private String contentEn;
    private String contentJa;
    private String contentKo;
    private String createdAt;
    // 이 메시지를 아직 안 읽은 참여자 수(발신자 본인 제외). 카카오톡처럼 0이 되면
    // 프론트에서 배지를 숨기는 방식으로 쓴다.
    private Integer unreadCount;

    // 원문 언어(ko/en/ja, 글자로 판별). 화면 언어와 다를 때만 번역 버튼을 보여준다.
    public String getSrcLangCd() {
        return ContentLangUtil.detect(content);
    }
}
