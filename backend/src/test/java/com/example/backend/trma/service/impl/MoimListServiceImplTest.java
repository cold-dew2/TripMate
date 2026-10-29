package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.ChatMapper;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.service.NotificationPushService;
import com.example.backend.trma.service.TourListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[소모임] MoimListService")
class MoimListServiceImplTest {

    @Mock MoimListMapper moimListMapper;
    @Mock ChatMapper chatMapper;
    @Mock NotificationMapper notificationMapper;
    @Mock NotificationPushService notificationPushService;
    @Mock RestClient restClient;
    @Mock TourListService tourListService;
    MoimListServicelmpl service;

    static final String MOIM = "M0000000001";
    static final String HOST = "host@t.com";
    static final String USER = "user@t.com";

    @BeforeEach
    void setUp() {
        service = new MoimListServicelmpl(moimListMapper, chatMapper, notificationMapper, notificationPushService, restClient, tourListService);
    }

    @Nested
    @DisplayName("소모임 생성")
    class Create {
        @Test
        @DisplayName("모임, 모임장, 테마, 일정을 저장하고 M으로 시작하는 모임ID를 돌려준다")
        void createMoim() {
            CreateMoimRequest request = new CreateMoimRequest();
            request.setMoimTitle("세종 나들이");
            request.setMoimCateData(List.of(new MoimCateData(), new MoimCateData()));
            request.setMoimPlanData(List.of(new MoimPlanInsertData()));

            CreateMoimResponse response = service.createMoim(request, HOST);

            assertThat(response.isSuccess()).isTrue();
            String moimId = response.getData().getMoimId();
            assertThat(moimId).matches("M[0-9A-F]{10}");
            verify(moimListMapper).createMoimList(request, moimId, HOST);
            verify(moimListMapper).insertMoimMember(moimId, HOST, "A", "Y");
            verify(moimListMapper, times(2)).insertMoimCate(any(), eq(moimId), eq(HOST));
            verify(moimListMapper).insertMoimPlan(any(), eq(moimId), eq(HOST));
            verify(moimListMapper).updateMoimImgFromFirstPlan(moimId);
        }

        @Test
        @DisplayName("일정이 없으면 대표 이미지 갱신을 하지 않는다")
        void createMoim_noPlan() {
            CreateMoimRequest request = new CreateMoimRequest();

            assertThat(service.createMoim(request, HOST).isSuccess()).isTrue();
            verify(moimListMapper, never()).updateMoimImgFromFirstPlan(any());
        }

        @Test
        @DisplayName("DB 오류 시 500 실패 응답")
        void createMoim_dbError() {
            doThrow(new RuntimeException("db down")).when(moimListMapper).createMoimList(any(), any(), any());

            CreateMoimResponse response = service.createMoim(new CreateMoimRequest(), HOST);

            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getData()).isNull();
        }
    }

    @Nested
    @DisplayName("소모임 참여 신청")
    class Apply {
        @Test
        @DisplayName("대기(N) 상태로 등록하고 모임장에게 알림을 보낸다")
        void apply_success() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);

            ApplyMoimResponse response = service.applyMoim(MOIM, USER);

            assertThat(response.isSuccess()).isTrue();
            verify(moimListMapper).insertMoimMember(MOIM, USER, "M", "N");
            verify(notificationMapper).insertApplyNotification(MOIM, USER);
            verify(notificationPushService).pushToUser(HOST);
        }

        @Test
        @DisplayName("본인이 만든 모임에는 신청할 수 없다")
        void apply_self() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);

            assertThat(service.applyMoim(MOIM, HOST).getCode()).isEqualTo("SELF_APPLY");
            verify(moimListMapper, never()).insertMoimMember(any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 신청/가입했으면 409")
        void apply_already() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            when(moimListMapper.moimJoinStatus(MOIM, USER)).thenReturn(new MoimJoinStatusData());

            assertThat(service.applyMoim(MOIM, USER).getStatus()).isEqualTo(409);
            verify(moimListMapper, never()).insertMoimMember(any(), any(), any(), any());
        }

        @Test
        @DisplayName("동시 요청으로 유니크 제약에 걸려도 409로 친절하게 응답한다")
        void apply_duplicateKey() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            doThrow(new DuplicateKeyException("dup")).when(moimListMapper).insertMoimMember(MOIM, USER, "M", "N");

            assertThat(service.applyMoim(MOIM, USER).getCode()).isEqualTo("ALREADY_APPLIED");
        }

        @Test
        @DisplayName("정원이 이미 찼으면 신청을 받지 않는다(409 MOIM_FULL)")
        void apply_full() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            when(moimListMapper.moimMaxMemberForUpdate(MOIM)).thenReturn(4);
            when(moimListMapper.approvedMemberCount(MOIM)).thenReturn(4);

            assertThat(service.applyMoim(MOIM, USER).getCode()).isEqualTo("MOIM_FULL");
            verify(moimListMapper, never()).insertMoimMember(any(), any(), any(), any());
            verify(notificationMapper, never()).insertApplyNotification(any(), any());
        }

        @Test
        @DisplayName("정원에 여유가 있으면 신청을 받는다")
        void apply_notFull() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            when(moimListMapper.moimMaxMemberForUpdate(MOIM)).thenReturn(4);
            when(moimListMapper.approvedMemberCount(MOIM)).thenReturn(3);

            assertThat(service.applyMoim(MOIM, USER).isSuccess()).isTrue();
        }

        @Test
        @DisplayName("실시간 알림 푸시가 실패해도 신청은 성공한다")
        void apply_pushFails() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            doThrow(new RuntimeException("ws down")).when(notificationPushService).pushToUser(HOST);

            assertThat(service.applyMoim(MOIM, USER).isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("멤버 승인/거절")
    class Members {
        UpdateMoimMemberRequest approve(boolean value) {
            UpdateMoimMemberRequest request = new UpdateMoimMemberRequest();
            request.setApprove(value);
            return request;
        }

        @Test
        @DisplayName("모임장이 아니면 403")
        void notHost() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);

            assertThat(service.updateMoimMember(MOIM, USER, approve(true), "stranger@t.com").getStatus()).isEqualTo(403);
            verify(moimListMapper, never()).updateMoimMemberState(any(), any(), any(), any());
        }

        @Test
        @DisplayName("승인하면 상태를 Y로 바꾸고 채팅방에 합류시킨다")
        void approve_joinsChat() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            when(moimListMapper.moimMaxMemberForUpdate(MOIM)).thenReturn(4);
            when(moimListMapper.approvedMemberCount(MOIM)).thenReturn(2);

            assertThat(service.updateMoimMember(MOIM, USER, approve(true), HOST).isSuccess()).isTrue();
            verify(moimListMapper).updateMoimMemberState(MOIM, USER, "Y", HOST);
            verify(chatMapper).insertMemberIfNotExists("moim-" + MOIM, USER);
            verify(chatMapper).reactivateChatMember("moim-" + MOIM, USER);
        }

        @Test
        @DisplayName("정원이 찼으면 승인하지 않는다(409 MOIM_FULL)")
        void approve_full() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            when(moimListMapper.moimMaxMemberForUpdate(MOIM)).thenReturn(4);
            when(moimListMapper.approvedMemberCount(MOIM)).thenReturn(4);

            assertThat(service.updateMoimMember(MOIM, USER, approve(true), HOST).getCode()).isEqualTo("MOIM_FULL");
            verify(moimListMapper, never()).updateMoimMemberState(any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 승인된 멤버 재승인은 정원이 차 있어도 허용한다(멱등)")
        void approve_idempotent() {
            MoimJoinStatusData approved = new MoimJoinStatusData();
            approved.setStateCd("Y");
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            when(moimListMapper.moimMaxMemberForUpdate(MOIM)).thenReturn(4);
            when(moimListMapper.moimJoinStatus(MOIM, USER)).thenReturn(approved);

            assertThat(service.updateMoimMember(MOIM, USER, approve(true), HOST).isSuccess()).isTrue();
        }

        @Test
        @DisplayName("거절/추방하면 멤버를 삭제하고 채팅방에서는 추방 상태로 표시한다")
        void reject() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);

            assertThat(service.updateMoimMember(MOIM, USER, approve(false), HOST).isSuccess()).isTrue();
            verify(moimListMapper).deleteMoimMember(MOIM, USER);
            verify(chatMapper).markChatMemberKicked("moim-" + MOIM, USER);
        }
    }

    @Nested
    @DisplayName("삭제/일정 수정/후기")
    class Manage {
        @Test
        @DisplayName("삭제: 본인 모임이 아니면(영향 행 0) 403, 본인이면 채팅방 멤버도 정리")
        void delete() {
            when(moimListMapper.deleteMoim(MOIM, USER)).thenReturn(0);
            when(moimListMapper.deleteMoim(MOIM, HOST)).thenReturn(1);

            assertThat(service.deleteMoim(MOIM, USER).getStatus()).isEqualTo(403);
            verify(chatMapper, never()).deleteAllChatMembersByRoom(any());

            assertThat(service.deleteMoim(MOIM, HOST).isSuccess()).isTrue();
            verify(chatMapper).deleteAllChatMembersByRoom("moim-" + MOIM);
        }

        @Test
        @DisplayName("일정 수정: 종료일이 시작일보다 앞이면 거절한다")
        void updatePlan_invalidDate() {
            MoimDetailData detail = new MoimDetailData();
            detail.setMoimStartDt(LocalDate.of(2026, 11, 10));
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            when(moimListMapper.moimDetail(MOIM)).thenReturn(detail);
            UpdateMoimPlanRequest request = new UpdateMoimPlanRequest();
            request.setMoimEndDt("2026-11-09");

            assertThat(service.updateMoimPlan(MOIM, request, HOST).getCode()).isEqualTo("INVALID_DATE");
            verify(moimListMapper, never()).deleteMoimPlan(any());
        }

        @Test
        @DisplayName("일정 수정: 모임장만 가능하고, 기존 일정을 지우고 새로 저장한다")
        void updatePlan() {
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            UpdateMoimPlanRequest request = new UpdateMoimPlanRequest();
            request.setItems(List.of(new MoimPlanInsertData(), new MoimPlanInsertData()));

            assertThat(service.updateMoimPlan(MOIM, request, USER).getCode()).isEqualTo("FORBIDDEN");
            assertThat(service.updateMoimPlan(MOIM, request, HOST).isSuccess()).isTrue();
            verify(moimListMapper).deleteMoimPlan(MOIM);
            verify(moimListMapper, times(2)).insertMoimPlan(any(), eq(MOIM), eq(HOST));
        }

        @Test
        @DisplayName("후기: 여행이 끝나기 전(오늘 종료 포함)에는 등록할 수 없다")
        void review_notFinished() {
            MoimSearchData info = new MoimSearchData();
            info.setMoimEndDt(LocalDate.now());
            when(moimListMapper.moimInfo(MOIM)).thenReturn(info);

            assertThat(service.createMoimReview(MOIM, new CreateMoimReviewRequest(), USER).getCode()).isEqualTo("MOIM_NOT_FINISHED");
            verify(moimListMapper, never()).insertMoimReview(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("후기: 여행이 끝났으면 RVO로 시작하는 ID로 저장하고 이미지 URL은 쉼표로 합친다")
        void review_success() {
            MoimSearchData info = new MoimSearchData();
            info.setMoimEndDt(LocalDate.now().minusDays(1));
            when(moimListMapper.moimInfo(MOIM)).thenReturn(info);
            MoimJoinStatusData approved = new MoimJoinStatusData();
            approved.setStateCd("Y");
            when(moimListMapper.moimJoinStatus(MOIM, USER)).thenReturn(approved);
            CreateMoimReviewRequest request = new CreateMoimReviewRequest();
            request.setImageUrls(List.of("/a.png", "/b.png"));

            assertThat(service.createMoimReview(MOIM, request, USER).isSuccess()).isTrue();
            verify(moimListMapper).insertMoimReview(startsWith("RVO"), eq(MOIM), eq(request), eq("/a.png,/b.png"), eq(USER));
        }

        @Test
        @DisplayName("후기: 모임 멤버가 아니거나 승인 대기(N) 상태면 등록할 수 없다")
        void review_nonMember() {
            MoimSearchData info = new MoimSearchData();
            info.setMoimEndDt(LocalDate.now().minusDays(1));
            when(moimListMapper.moimInfo(MOIM)).thenReturn(info);
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);
            MoimJoinStatusData pending = new MoimJoinStatusData();
            pending.setStateCd("N");
            lenient().when(moimListMapper.moimJoinStatus(MOIM, "pending@t.com")).thenReturn(pending);

            assertThat(service.createMoimReview(MOIM, new CreateMoimReviewRequest(), "stranger@t.com").getCode()).isEqualTo("NOT_MEMBER");
            assertThat(service.createMoimReview(MOIM, new CreateMoimReviewRequest(), "pending@t.com").getCode()).isEqualTo("NOT_MEMBER");
            verify(moimListMapper, never()).insertMoimReview(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("후기: 모임장은 후기를 남길 수 있다")
        void review_host() {
            MoimSearchData info = new MoimSearchData();
            info.setMoimEndDt(LocalDate.now().minusDays(1));
            when(moimListMapper.moimInfo(MOIM)).thenReturn(info);
            when(moimListMapper.moimHostUserId(MOIM)).thenReturn(HOST);

            assertThat(service.createMoimReview(MOIM, new CreateMoimReviewRequest(), HOST).isSuccess()).isTrue();
        }
    }
}
