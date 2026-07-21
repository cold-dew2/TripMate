// 회원 관련 예외 처리

// 1. package
package com.example.backend.trma.exception;

// 2. import


// 3. 클래스 선언
//public class MemberException extends RuntimeException {
//
//    // 4. MemberErrorCode 저장
//
//    // 5. 생성자
//
//    // 6. Getter
//}

/**
 * MemberException 역할
 *
 * 회원 관련 예외를 발생시킨다.
 *
 * 사용 예시
 *
 * throw new MemberException(
 *     MemberErrorCode.DUPLICATE_USER_ID
 * );
 *
 * 처리 순서
 *
 * Service
 * ↓
 * 예외 발생
 * ↓
 * MemberException 생성
 * ↓
 * GlobalExceptionHandler 처리
 * ↓
 * ErrorResponse 반환
 */


public class MemberException extends RuntimeException {

    private final MemberErrorCode errorCode;

    public MemberException(
            MemberErrorCode errorCode
    ) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public MemberErrorCode getErrorCode() {
        return errorCode;
    }
}