package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.ChatMessageData;
import com.example.backend.trma.dto.dataList.ChatRoomData;
import com.example.backend.trma.dto.dataList.ChatUnreadDeltaData;
import com.example.backend.trma.dto.dataList.NewChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMapper {
    //채팅방 생성(없을 때만)
    int insertRoomIfNotExists(@Param("roomId") String roomId,
                              @Param("title") String title,
                              @Param("userId") String userId);

    //채팅방 멤버 등록(없을 때만)
    int insertMemberIfNotExists(@Param("roomId") String roomId,
                                @Param("userId") String userId);

    //내 채팅방 목록 조회
    List<ChatRoomData> chatRooms(String userId);

    //채팅방 멤버 계정 목록 조회
    List<String> roomMemberIds(String roomId);

    //채팅 메시지 목록 조회
    List<ChatMessageData> chatMessages(String roomId);

    //채팅 메시지 등록
    int insertMessage(NewChatMessage message);

    //메시지 상세 조회(발신자명 포함)
    ChatMessageData messageDetail(long messageId);

    //채팅 메시지 번역 캐시 저장
    int updateMessageTranslation(@Param("messageId") long messageId,
                                  @Param("contentEn") String contentEn,
                                  @Param("contentJa") String contentJa);

    //전체 안읽은 메시지 수
    int unreadTotal(String userId);

    //채팅방 읽음 처리
    int updateLastRead(@Param("roomId") String roomId, @Param("userId") String userId);

    //소모임 채팅방(roomId=moim-{moimId})의 기본 제목으로 쓸 모임 이름 조회
    String moimTitle(String moimId);

    //예전 버그로 roomId 문자열이 그대로 제목으로 저장된 방을 다시 들어왔을 때 바로잡음
    int fixRoomTitleIfStale(@Param("roomId") String roomId, @Param("title") String title);

    //채팅방 나가기(멤버 행 삭제)
    int deleteChatMember(@Param("roomId") String roomId, @Param("userId") String userId);

    //모임에서 추방/거절됐을 때 채팅방 멤버 상태를 추방으로 표시(행은 남겨둠)
    int markChatMemberKicked(@Param("roomId") String roomId, @Param("userId") String userId);

    //채팅방에서 나의 참여 상태 조회(정상 'A' / 추방됨 'K')
    String chatMemberState(@Param("roomId") String roomId, @Param("userId") String userId);

    //읽음 처리 후, 이 사용자가 읽음으로써 안읽은 사람 수가 바뀔 수 있는 메시지들
    //(자신이 보낸 메시지 제외)의 최신 안읽은 사람 수 조회
    List<ChatUnreadDeltaData> unreadCountsForRoom(@Param("roomId") String roomId, @Param("userId") String userId);
}
