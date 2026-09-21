// 예외를 잡아서 프론트가 보기 좋은 JSON으로 바꿔주는 곳
package com.example.backend.global.exception;

import com.example.backend.trma.exception.MemberException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 검증 실패 처리 (예: 회원가입 시 빈 아이디/짧은 비밀번호).
     * 이게 없으면 아래 범용 Exception 핸들러가 잡아 500 + "서버 내부 오류"로
     * 응답해버려서, 입력값이 잘못됐을 뿐인데 서버 오류처럼 보였다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("입력값을 확인해주세요.");

        log.warn("입력값 검증 실패: {}", message);

        return ResponseEntity
                .badRequest()
                .body(
                        ErrorResponse.builder()
                                .success(false)
                                .code("VALIDATION_FAILED")
                                .message(message)
                                .build()
                );
    }

    /**
     * 회원 관련 예외 처리
     *
     * 예)
     * throw new MemberException(...)
     *
     * 발생 시 여기서 잡는다.
     */
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ErrorResponse> handleMemberException(
            MemberException e
    ) {

        // 로그 출력
        log.warn(
                "Member Exception : {}",
                e.getMessage()
        );

        // 에러 응답 생성 후 반환
        return ResponseEntity
                .status(
                        e.getErrorCode().getStatus()
                )
                .body(
                        ErrorResponse.builder()
                                .success(false)
                                .message(
                                        e.getErrorCode().getMessage()
                                )
                                .build()
                );
    }

    /**
     * 예상하지 못한 서버 오류 처리
     *
     * NullPointerException
     * DB 오류
     * 기타 RuntimeException
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e
    ) {

        // 에러 로그 출력
        log.error(
                "Unexpected Exception",
                e
        );

        // 사용자에게는 공통 메시지 반환
        return ResponseEntity
                .internalServerError()
                .body(
                        ErrorResponse.builder()
                                .success(false)
                                .message(
                                        "서버 내부 오류가 발생했습니다."
                                )
                                .build()
                );
    }
}