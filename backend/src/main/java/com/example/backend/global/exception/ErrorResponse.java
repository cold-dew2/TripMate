package com.example.backend.global.exception;

import lombok.Builder;

/**
 * 공통 에러 응답 DTO
 */
@Builder
public record ErrorResponse(

    /**
     * 요청 성공 여부
     */
    boolean success,

    /**
     * 에러 코드
     */
    String code,

    /**
     * 사용자에게 보여줄 메시지
     */
    String message

) {
}