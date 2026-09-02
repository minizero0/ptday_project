package com.gym.common.exception;

/**
 * 비즈니스 규칙 위반을 나타내는 예외. ErrorCode 를 품고 다닌다 (CLAUDE.md §7).
 * Service 에서 규칙 위반 지점에 던지고, 전역 예외 핸들러가 잡아 통일 응답으로 변환한다.
 * RuntimeException 상속: @Transactional 자동 롤백 + 호출부 try-catch 강제 없음.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
