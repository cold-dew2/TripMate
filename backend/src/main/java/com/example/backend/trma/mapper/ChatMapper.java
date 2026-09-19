package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.ChatMessageData;
import com.example.backend.trma.dto.dataList.ChatRoomData;
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

    //전체 안읽은 메시지 수
    int unreadTotal(String userId);

    //채팅방 읽음 처리
    int updateLastRead(@Param("roomId") String roomId, @Param("userId") String userId);
}
