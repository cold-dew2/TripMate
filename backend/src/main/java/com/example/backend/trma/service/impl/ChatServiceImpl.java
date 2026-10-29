package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.ChatMessageData;
import com.example.backend.trma.dto.dataList.ChatRoomData;
import com.example.backend.trma.dto.dataList.ChatMessageTranslationData;
import com.example.backend.trma.dto.dataList.ChatUnreadDeltaData;
import com.example.backend.trma.dto.dataList.GeminiData;
import com.example.backend.trma.dto.dataList.MoimJoinStatusData;
import com.example.backend.trma.dto.dataList.MoimTitleTranslationData;
import com.example.backend.trma.dto.dataList.NewChatMessage;
import com.example.backend.trma.dto.request.GeminiRequest;
import com.example.backend.trma.dto.request.SendChatMessageRequest;
import com.example.backend.trma.dto.response.ChatMessagesResponse;
import com.example.backend.trma.dto.response.ChatRoomsResponse;
import com.example.backend.trma.dto.response.GeminiResponse;
import com.example.backend.trma.dto.response.SendChatMessageResponse;
import com.example.backend.trma.dto.response.LeaveChatRoomResponse;
import com.example.backend.trma.dto.response.MarkChatReadResponse;
import com.example.backend.trma.dto.response.TranslateChatMessageResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;
import com.example.backend.trma.mapper.ChatMapper;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.service.ChatService;
import com.example.backend.trma.service.NotificationPushService;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.util.AiJsonUtil;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.example.backend.trma.util.AiErrorUtil;
import com.example.backend.trma.util.ContentLangUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMapper chatMapper;
    private final MoimListMapper moimListMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationPushService notificationPushService;
    private final SimpMessagingTemplate broker;
    private final RestClient restClient;
    private final TourListService tourListService;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    //내 채팅방 목록 조회
    @Override
    public ChatRoomsResponse chatRooms(String userId, String lang) {

        try {
            List<ChatRoomData> rooms = accessibleRooms(userId);
            // 다른 목록 화면과 동일하게, 캐시(소모임 제목 캐시 재사용 포함)로 못 채우고
            // Gemini를 불러야 하는 방이 있어도 최대 0.5초만 기다린다.
            try {
                CompletableFuture.runAsync(() -> applyChatRoomTranslations(rooms, lang)).get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("채팅방 목록 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. lang={}", lang);
            } catch (Exception e) {
                log.warn("채팅방 목록 번역 대기 중 오류가 발생했습니다. lang={}", lang, e);
            }

            return new ChatRoomsResponse(
                    true,
                    200,
                    "SUCCESS",
                    "채팅방 목록을 정상적으로 조회했습니다.",
                    "/chat/rooms",
                    "",
                    rooms
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new ChatRoomsResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/chat/rooms",
                    "",
                    null
            );
        }
    }

    //채팅 메시지 목록 조회(조회 시 자동 입장 처리)
    @Override
    public ChatMessagesResponse chatMessages(String roomId, String userId) {

        try {
            if (!canReadRoom(roomId, userId)) {
                return new ChatMessagesResponse(
                        false,
                        403,
                        "NOT_MEMBER",
                        "이 채팅방의 멤버만 대화를 볼 수 있습니다.",
                        "/chat/rooms/" + roomId + "/messages",
                        "",
                        null,
                        0,
                        null
                );
            }

            String resolvedTitle = resolveRoomTitle(roomId, null);
            chatMapper.insertRoomIfNotExists(roomId, resolvedTitle, userId);
            chatMapper.fixRoomTitleIfStale(roomId, resolvedTitle);
            chatMapper.insertMemberIfNotExists(roomId, userId);

            // 메시지를 읽기 전에 먼저 읽음 처리를 해야, 지금 막 보는 메시지들의
            // "안읽은 사람 수"에 자기 자신이 포함되는 걸 막을 수 있다. 다른 참여자들에게는
            // 전체 메시지를 다시 보내는 대신 바뀐 안읽음 수만 가볍게 실어 보낸다.
            markReadAndBroadcastDelta(roomId, userId);

            // 번역은 미리 하지 않는다(사용자가 메시지별로 요청할 때 translateMessage에서 번역·저장).
            List<ChatMessageData> messages = chatMapper.chatMessages(roomId);
            int memberCount = chatMapper.roomMemberIds(roomId).size();
            String myState = chatMapper.chatMemberState(roomId, userId);

            return new ChatMessagesResponse(
                    true,
                    200,
                    "SUCCESS",
                    "채팅 메시지를 정상적으로 조회했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    messages,
                    memberCount,
                    myState
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new ChatMessagesResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    null,
                    0,
                    null
            );
        }
    }

    //채팅 메시지 등록
    @Override
    public SendChatMessageResponse sendMessage(String roomId, SendChatMessageRequest request, String userId) {

        try {
            if (!isActiveMember(roomId, userId)) {
                return new SendChatMessageResponse(
                        false,
                        403,
                        "NOT_MEMBER",
                        "이 채팅방의 멤버만 메시지를 보낼 수 있습니다.",
                        "/chat/rooms/" + roomId + "/messages",
                        "",
                        null
                );
            }

            String title = resolveRoomTitle(roomId, request.getTitle());
            chatMapper.insertRoomIfNotExists(roomId, title, userId);
            chatMapper.insertMemberIfNotExists(roomId, userId);

            if ("K".equals(chatMapper.chatMemberState(roomId, userId))) {
                return new SendChatMessageResponse(
                        false,
                        403,
                        "KICKED",
                        "이 채팅방에서 추방되어 메시지를 보낼 수 없습니다.",
                        "/chat/rooms/" + roomId + "/messages",
                        "",
                        null
                );
            }

            // 프론트는 빈/공백 메시지를 막지만, 그건 API를 직접 호출하면 우회할 수 있다.
            // DB 컬럼은 NOT NULL일 뿐 빈 문자열은 그대로 허용하므로 서버에서도 막는다.
            if (request.getContent() == null || request.getContent().isBlank()) {
                return new SendChatMessageResponse(
                        false,
                        400,
                        "EMPTY_CONTENT",
                        "메시지 내용을 입력해주세요.",
                        "/chat/rooms/" + roomId + "/messages",
                        "",
                        null
                );
            }

            NewChatMessage newMessage = new NewChatMessage();
            newMessage.setRoomId(roomId);
            newMessage.setSenderId(userId);
            newMessage.setContent(request.getContent());
            chatMapper.insertMessage(newMessage);

            ChatMessageData saved = chatMapper.messageDetail(newMessage.getMessageId());
            broker.convertAndSend("/topic/chat/" + roomId, saved);
            // 알림 등록/실시간 푸시는 부가 기능이라 여기서 실패해도 메시지 전송 자체는
            // 이미 완료된 것으로 처리해야 하므로, 별도로 감싸서 전송 성공 여부에 영향을 주지 않게 한다.
            try {
                notificationMapper.insertChatNotifications(roomId, userId, saved.getSenderName(), request.getContent());
                for (String memberId : chatMapper.roomMemberIds(roomId)) {
                    if (!memberId.equals(userId)) {
                        notificationPushService.pushToUser(memberId);
                    }
                }
            } catch (Exception e) {
                log.warn("채팅 알림 등록/푸시에 실패했습니다. roomId={}", roomId, e);
            }

            return new SendChatMessageResponse(
                    true,
                    200,
                    "SUCCESS",
                    "메시지를 전송했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    saved
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new SendChatMessageResponse(
                    false,
                    500,
                    "FAIL",
                    "메시지 전송 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    null
            );
        }
    }

    //전체 안읽은 메시지 수 조회
    @Override
    public UnreadCountResponse unreadTotal(String userId) {

        try {
            // 목록과 같은 기준(열 수 있는 방)으로 합산해야, 열 수 없는 방의 안읽음이
            // 배지에 영원히 남지 않는다.
            int total = accessibleRooms(userId).stream().mapToInt(ChatRoomData::getUnreadCount).sum();

            return new UnreadCountResponse(
                    true,
                    200,
                    "SUCCESS",
                    "안읽은 메시지 수를 정상적으로 조회했습니다.",
                    "/chat/unreadCount",
                    "",
                    total
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new UnreadCountResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/chat/unreadCount",
                    "",
                    0
            );
        }
    }

    //채팅방 나가기(소모임 채팅방이면 모임 탈퇴까지 함께 처리)
    @Override
    public LeaveChatRoomResponse leaveChatRoom(String roomId, String userId) {

        try {
            String moimId = extractMoimId(roomId);

            // 모임장이 나가버리면 모임 자체가 주인 없이 남기 때문에, 이 흐름으로는
            // 나갈 수 없게 막는다.
            if (moimId != null && userId.equals(moimListMapper.moimHostUserId(moimId))) {
                return new LeaveChatRoomResponse(
                        false,
                        400,
                        "HOST_CANNOT_LEAVE",
                        "모임장은 채팅방을 나갈 수 없습니다.",
                        "/chat/rooms/" + roomId
                );
            }

            chatMapper.deleteChatMember(roomId, userId);
            if (moimId != null) {
                moimListMapper.deleteMoimMember(moimId, userId);
            }

            return new LeaveChatRoomResponse(
                    true,
                    200,
                    "SUCCESS",
                    "채팅방에서 나갔습니다.",
                    "/chat/rooms/" + roomId
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new LeaveChatRoomResponse(
                    false,
                    500,
                    "FAIL",
                    "처리 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId
            );
        }
    }

    //채팅방 읽음 처리만 가볍게(전체 메시지 재조회 없이 안읽음 수 델타만 반환/방송)
    @Override
    public MarkChatReadResponse markRead(String roomId, String userId) {

        try {
            if (!canReadRoom(roomId, userId)) {
                return new MarkChatReadResponse(
                        false,
                        403,
                        "NOT_MEMBER",
                        "이 채팅방의 멤버가 아닙니다.",
                        "/chat/rooms/" + roomId + "/read",
                        null
                );
            }

            List<ChatUnreadDeltaData> delta = markReadAndBroadcastDelta(roomId, userId);

            return new MarkChatReadResponse(
                    true,
                    200,
                    "SUCCESS",
                    "읽음 처리했습니다.",
                    "/chat/rooms/" + roomId + "/read",
                    delta
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MarkChatReadResponse(
                    false,
                    500,
                    "FAIL",
                    "처리 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId + "/read",
                    null
            );
        }
    }

    // 방을 읽음 처리하고, 그로 인해 안읽음 수가 바뀔 수 있는 메시지들의 최신 카운트를
    // 계산해서 다른 참여자들에게 방송한다. 전체 메시지(본문/발신자 등)를 다시 보내지
    // 않고 "메시지ID + 새 카운트" 쌍만 보내는 게 핵심 — chatMessages(최초 조회)와
    // markRead(이후 갱신) 양쪽에서 공유한다.
    private List<ChatUnreadDeltaData> markReadAndBroadcastDelta(String roomId, String userId) {
        chatMapper.updateLastRead(roomId, userId);
        // 알림 목록을 거치지 않고 채팅방에 바로 들어온 경우에도 그 방의 채팅 알림을
        // 읽음 처리해서, 홈 화면 알림 배지가 계속 남아있지 않도록 한다.
        notificationMapper.markChatNotificationsRead(roomId, userId);

        List<ChatUnreadDeltaData> delta = chatMapper.unreadCountsForRoom(roomId, userId);
        // 지금 이 방을 열어둔 다른 참여자들도(그리고 나 자신도) "안읽은 사람 수" 배지가
        // 실시간으로 줄어드는 걸 보도록, 새 메시지 토픽과는 별개의 채널로 보낸다.
        broker.convertAndSend("/topic/chat/" + roomId + "/read", delta);
        return delta;
    }

    // 예전에는 조회/전송 시 호출한 사람을 무조건 채팅방 멤버로 자동 등록해서, 로그인한
    // 사용자라면 누구나 URL의 "moim-{모임ID}"만 알면 남의 소모임 대화를 읽고 쓸 수 있었다.
    // 소모임 채팅방은 모임장과 승인된(Y) 멤버만 참여할 수 있게 한다.
    private boolean isActiveMember(String roomId, String userId) {
        if (roomId == null || userId == null || userId.isBlank()) return false;

        String moimId = extractMoimId(roomId);
        if (moimId == null) {
            // 소모임이 아닌 방(현재 프론트에는 없음)은 이미 멤버로 들어가 있는 사람만 허용한다.
            return chatMapper.roomMemberIds(roomId).contains(userId);
        }
        if (userId.equals(moimListMapper.moimHostUserId(moimId))) return true;
        MoimJoinStatusData status = moimListMapper.moimJoinStatus(moimId, userId);
        return status != null && "Y".equals(status.getStateCd());
    }

    // 예전에는 조회만 해도 채팅방 멤버로 자동 등록됐기 때문에, 모임 멤버가 아닌데도
    // 채팅방 멤버 행이 남아 있는 경우가 있다(예: 승인 전에 방에 들어갔던 신청자). 그런 방은
    // 열어도 403이므로 목록/안읽음 수에서 뺀다. DB 행은 지우지 않아서, 나중에 승인되면 그대로 다시 보인다.
    private List<ChatRoomData> accessibleRooms(String userId) {
        return chatMapper.chatRooms(userId).stream()
                .filter(room -> canReadRoom(room.getRoomId(), userId))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    // 추방된 멤버(K)는 모임 멤버에선 빠지지만, 기존 화면처럼 대화 기록과 "추방됨" 안내는
    // 볼 수 있게 두고 전송만 막는다(sendMessage는 isActiveMember로 확인).
    @Override
    public boolean canReadRoom(String roomId, String userId) {
        if (isActiveMember(roomId, userId)) return true;
        return userId != null && "K".equals(chatMapper.chatMemberState(roomId, userId));
    }

    private String extractMoimId(String roomId) {
        return (roomId != null && roomId.startsWith("moim-")) ? roomId.substring("moim-".length()) : null;
    }

    // 프론트가 방 제목을 안 넘긴 채(예: 채팅 목록에서 바로 진입, 알림 클릭 등) 방을 처음
    // 만들게 되면 roomId 문자열("moim-XXXX")이 그대로 제목으로 저장돼버리던 문제가 있었다.
    // roomId가 "moim-{moimId}" 형식인 소모임 채팅방은 실제 모임 이름을 기본 제목으로 쓴다.
    private String resolveRoomTitle(String roomId, String requestedTitle) {
        if (requestedTitle != null && !requestedTitle.isBlank()) {
            return requestedTitle;
        }

        if (roomId != null && roomId.startsWith("moim-")) {
            String moimId = roomId.substring("moim-".length());
            String moimTitle = chatMapper.moimTitle(moimId);
            if (moimTitle != null && !moimTitle.isBlank()) {
                return moimTitle;
            }
        }

        return roomId;
    }

    // ========================= 채팅방 제목 번역 =========================
    // 모임 제목/후기와 동일한 "조회 시점에 요청 언어로만 번역해서 DB에 캐시(재사용)" 방식.
    // 채팅 메시지처럼 전송 시점에 두 언어를 미리 다 만들어둘 필요는 없다 — 방 제목은
    // 거의 바뀌지 않고, 그 방을 실제로 보는 언어로만 번역해두면 충분하기 때문이다.
    private void applyChatRoomTranslations(List<ChatRoomData> rooms, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (rooms == null || rooms.isEmpty()) return;

        List<ChatRoomData> uncached = new ArrayList<>();
        for (ChatRoomData room : rooms) {
            String cached = "en".equals(lang) ? room.getTitleEn() : room.getTitleJa();
            if (cached != null && !cached.isBlank()) {
                room.setTitle(cached);
            } else {
                uncached.add(room);
            }
        }
        if (uncached.isEmpty()) return;

        // 소모임 채팅방(roomId="moim-{moimId}")의 제목은 그 모임 이름과 같다. 소모임
        // 목록/상세를 먼저 봤다면 MOIM_TITLE_EN/JA에 이미 번역이 캐시돼 있는 경우가
        // 많으므로, Gemini를 또 호출하기 전에 그 캐시를 재사용한다(같은 문장을 두 번
        // 번역하는 토큰 낭비를 막고, 채팅방 목록 로딩도 그만큼 빨라진다). 재사용한
        // 값은 채팅방 자체의 캐시(TITLE_EN/JA)에도 저장해 다음부터는 이 조회조차 필요 없게 한다.
        Map<String, String> moimIdByRoomId = new HashMap<>();
        for (ChatRoomData room : uncached) {
            if (room.getRoomId() != null && room.getRoomId().startsWith("moim-")) {
                moimIdByRoomId.put(room.getRoomId(), room.getRoomId().substring("moim-".length()));
            }
        }
        if (!moimIdByRoomId.isEmpty()) {
            List<MoimTitleTranslationData> moims = moimListMapper.moimTitlesByIds(new ArrayList<>(moimIdByRoomId.values()));
            Map<String, String> moimTitleByMoimId = new HashMap<>();
            for (MoimTitleTranslationData moim : moims) {
                String moimCached = "en".equals(lang) ? moim.getMoimTitleEn() : moim.getMoimTitleJa();
                if (moimCached != null && !moimCached.isBlank()) {
                    moimTitleByMoimId.put(moim.getMoimId(), moimCached);
                }
            }

            List<ChatRoomData> stillUncached = new ArrayList<>();
            for (ChatRoomData room : uncached) {
                String moimId = moimIdByRoomId.get(room.getRoomId());
                String reused = moimId != null ? moimTitleByMoimId.get(moimId) : null;
                if (reused != null) {
                    chatMapper.updateRoomTitleTranslation(
                            room.getRoomId(),
                            "en".equals(lang) ? reused : null,
                            "ja".equals(lang) ? reused : null
                    );
                    room.setTitle(reused);
                } else {
                    stillUncached.add(room);
                }
            }
            uncached = stillUncached;
        }
        if (uncached.isEmpty()) return;

        try {
            StringBuilder listPrompt = new StringBuilder();
            for (ChatRoomData room : uncached) {
                listPrompt.append("[roomId %s]\n제목: %s\n\n".formatted(room.getRoomId(), room.getTitle()));
            }

            String langLabel = "en".equals(lang) ? "영어" : "일본어";
            String prompt = "다음은 채팅방 제목 목록입니다. 각 제목을 " + langLabel + "로 자연스럽게 번역해주세요.\n"
                    + "반드시 JSON 형식으로만 응답하고, 요청받은 roomId를 그대로 포함해서 응답하세요.\n"
                    + "[응답 형식]\n"
                    + "{\"chatRoomTranslations\":[{\"roomId\":\"R0001\",\"title\":\"번역된 제목\"}]}\n"
                    + "[채팅방 목록]\n" + listPrompt;

            GeminiData result = callGeminiForRoomTitleTranslations(prompt);
            if (result == null || result.getChatRoomTranslations() == null) return;

            Map<String, String> translatedTitles = new HashMap<>();
            for (GeminiData.ChatRoomTranslationItem item : result.getChatRoomTranslations()) {
                if (item.getRoomId() != null && item.getTitle() != null) {
                    translatedTitles.put(item.getRoomId(), item.getTitle());
                }
            }

            for (ChatRoomData room : uncached) {
                String title = translatedTitles.get(room.getRoomId());
                if (title == null || title.isBlank()) continue;

                chatMapper.updateRoomTitleTranslation(
                        room.getRoomId(),
                        "en".equals(lang) ? title : null,
                        "ja".equals(lang) ? title : null
                );
                room.setTitle(title);
            }
        } catch (Exception e) {
            log.warn("채팅방 제목 번역에 실패해 한국어로 표시합니다. lang={}", lang, e);
        }
    }

    private GeminiData callGeminiForRoomTitleTranslations(String prompt) {
        GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));
        GeminiResponse response = callGemini(geminiRequest);

        String aiResult = "";
        if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
            aiResult = response.candidates().get(0).content().parts().get(0).text();
        }
        if (aiResult == null || aiResult.isBlank()) return null;

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(AiJsonUtil.extractJson(aiResult), GeminiData.class);
    }

    // ========================= 채팅 메시지 번역 =========================
    // 이 앱은 다국어 지원이 핵심이라, AI가 끊겨도 이미 번역된 메시지는 계속 보여야 한다.
    // 그래서 전송 시점에 영어/일본어 번역을 한 번만 만들어 TB_TRMA_CHAT_MESSAGE에
    // 캐시해두고(다른 화면들의 "최초 조회 시 번역 후 캐시" 패턴과 동일), 조회 시에는
    // 이미 캐시된 값을 그대로 내려준다.

    private GeminiResponse callGemini(GeminiRequest geminiRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            System.out.println("[Gemini 요청] " + objectMapper.writeValueAsString(geminiRequest));
        } catch (Exception ignored) {
        }

        // Gemini가 일시적으로 과부하(503)이거나 요청이 몰려 제한(429)에 걸리는 경우가 잦아,
        // 한 번 실패했다고 바로 포기하면 메시지/방 제목 번역이 자주 안 되고 한국어 원문만
        // 보이는 문제가 있었다. 과부하성 오류에 한해 짧게 두 번 더 재시도한다.
        RestClientException lastError = null;
        // 사용량 한도 초과로 쉬는 중이면 호출하지 않고 바로 실패 처리한다.
        AiErrorUtil.throwIfQuotaBlocked();
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                GeminiResponse response = restClient.post()
                        .uri(geminiUrl)
                        .header("X-goog-api-key", geminiApiKey)
                        .body(geminiRequest)
                        .retrieve()
                        .body(GeminiResponse.class);

                try {
                    System.out.println("[Gemini 응답] " + objectMapper.writeValueAsString(response));
                } catch (Exception ignored) {
                }
                return response;
            } catch (RestClientException e) {
                lastError = e;
                // 사용량 한도 초과(일일 한도, 긴 retryDelay)는 짧은 재시도로 풀리지 않으므로
                // 바로 포기하고, Gemini가 알려준 시간 동안 호출을 쉰다.
                long cooldownMillis = AiErrorUtil.quotaCooldownMillis(e);
                if (cooldownMillis > 0) {
                    AiErrorUtil.markQuotaBlocked(cooldownMillis);
                    log.warn("Gemini 사용량 한도를 초과해 {}초 동안 호출을 건너뜁니다.", cooldownMillis / 1000);
                    throw e;
                }
                if (attempt == 4 || !AiErrorUtil.isRetryable(e)) throw e;
                log.warn("Gemini 호출이 일시적으로 실패해 재시도합니다({}/3).", attempt, e);
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw lastError;
    }

    // 같은 메시지·언어 번역이 동시에 여러 번 요청돼도 AI는 한 번만 부르고 결과를 나눠 쓴다.
    private final Map<String, CompletableFuture<String>> inFlightTranslations = new ConcurrentHashMap<>();
    private static final long CHAT_TRANSLATE_WAIT_SECONDS = 40;

    // 메시지 한 건을 요청한 언어로 번역한다(요청 시 번역). 저장된 번역이 검증을 통과하면 그대로 쓰고,
    // 없을 때만 번역해 언어별 컬럼에 저장한다. AI가 실패해도 오류가 아니라 FAILED 상태와 원문을 돌려준다.
    @Override
    public TranslateChatMessageResponse translateMessage(String roomId, long messageId, String lang, String userId) {
        String path = "/chat/rooms/" + roomId + "/messages/" + messageId + "/translate";
        try {
            if (!ContentLangUtil.isSupported(lang)) {
                return new TranslateChatMessageResponse(false, 400, "UNSUPPORTED_LANG", "지원하지 않는 언어입니다.", path, null);
            }
            if (!canReadRoom(roomId, userId)) {
                return new TranslateChatMessageResponse(false, 403, "NOT_MEMBER", "이 채팅방의 멤버만 번역할 수 있습니다.", path, null);
            }
            ChatMessageData message = chatMapper.messageDetail(messageId);
            if (message == null || !roomId.equals(message.getRoomId())) {
                return new TranslateChatMessageResponse(false, 404, "NOT_FOUND", "메시지를 찾을 수 없습니다.", path, null);
            }

            String source = message.getContent();
            String id = message.getMessageId();
            if (!ContentLangUtil.needsTranslation(source, lang)) {
                return translated(path, new ChatMessageTranslationData(id, lang, source, "SOURCE"));
            }
            String cached = ContentLangUtil.displayFromCache(source, lang,
                    message.getContentKo(), message.getContentEn(), message.getContentJa());
            if (cached != null) {
                return translated(path, new ChatMessageTranslationData(id, lang, cached, "CACHED"));
            }

            String result = translateOnce(messageId, source, lang);
            return result == null
                    ? translated(path, new ChatMessageTranslationData(id, lang, source, "FAILED"))
                    : translated(path, new ChatMessageTranslationData(id, lang, result, "TRANSLATED"));
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new TranslateChatMessageResponse(false, 500, "FAIL", "번역 중 오류가 발생했습니다.", path, null);
        }
    }

    private TranslateChatMessageResponse translated(String path, ChatMessageTranslationData data) {
        return new TranslateChatMessageResponse(true, 200, "SUCCESS", "메시지 번역을 조회했습니다.", path, data);
    }

    // 실패하면 null. 번역 검증은 translateFreeTexts가 한다(원문 그대로/다른 언어 응답은 버려짐).
    private String translateOnce(long messageId, String source, String lang) {
        String key = messageId + "#" + lang;
        CompletableFuture<String> created = new CompletableFuture<>();
        CompletableFuture<String> running = inFlightTranslations.putIfAbsent(key, created);
        if (running == null) {
            running = created;
            CompletableFuture.runAsync(() -> {
                try {
                    String text = tourListService.translateFreeTexts(Map.of("content", source), lang).get("content");
                    if (text != null) chatMapper.updateMessageTranslationLang(messageId, lang.toUpperCase(), text);
                    created.complete(text);
                } catch (Exception e) {
                    log.warn("채팅 메시지 번역에 실패했습니다. messageId={}, lang={}", messageId, lang, e);
                    created.complete(null);
                } finally {
                    inFlightTranslations.remove(key, created);
                }
            });
        }
        try {
            return running.get(CHAT_TRANSLATE_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("채팅 메시지 번역 대기 시간이 지났습니다. messageId={}, lang={}", messageId, lang);
            return null;
        }
    }
}
