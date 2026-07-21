// 회원 관련 에러 코드를 관리

// 1. package
package com.example.backend.trma.exception;

// 2. import
import org.springframework.http.HttpStatus;

// 3. enum 선언
// MemberErrorCode

// public enum MemberErrorCode {

    // 4. 에러 코드 목록
    // MEMBER_NOT_FOUND
    // DUPLICATE_USER_ID
    // DUPLICATE_EMAIL

    // 5. 상태코드
    // HttpStatus

    // 6. 에러 메시지
    // message

    // 7. 생성자

    // 8. Getter
// }

/**
 * MemberErrorCode 역할
 *
 * 회원 관련 에러를 관리한다.
 *
 * 사용 예시
 *
 * 아이디 중복
 * ↓
 * DUPLICATE_USER_ID
 *
 * 이메일 중복
 * ↓
 * DUPLICATE_EMAIL
 *
 * 회원 없음
 * ↓
 * MEMBER_NOT_FOUND
 *
 * 처리 순서
 *
 * Service
 * ↓
 * 에러 발생
 * ↓
 * MemberErrorCode 선택
 * ↓
 * MemberException 전달
 */

public enum MemberErrorCode {

    MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "회원을 찾을 수 없습니다."
    ),

    DUPLICATE_USER_ID(
            HttpStatus.CONFLICT,
            "이미 존재하는 아이디입니다."
    ),

    DUPLICATE_EMAIL(
            HttpStatus.CONFLICT,
            "이미 존재하는 이메일입니다."
    );

    private final HttpStatus status;
    private final String message;

    MemberErrorCode(
            HttpStatus status,
            String message
    ) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}