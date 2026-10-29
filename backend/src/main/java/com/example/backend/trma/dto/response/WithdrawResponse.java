package com.example.backend.trma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WithdrawResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    // 탈퇴를 막는 모임(진행 중이고 다른 승인 멤버가 있는 내가 만든 모임) 제목
    private List<String> blockingMoims;
}
