package com.gym.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 앱 전역 에러 사전. 코드(enum 이름)·HTTP 상태·기본 메시지를 한곳에서 관리한다 (CLAUDE.md §7).
 * 새 에러 유형이 생기면 여기에 항목을 추가한다.
 */
public enum ErrorCode {

    // 공통
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    DUPLICATE_MEMBER_NO(HttpStatus.CONFLICT, "이미 존재하는 회원번호입니다."),

    // 이용권
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "이용권을 찾을 수 없습니다."),
    MEMBERSHIP_END_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "만료일을 입력하세요."),
    INVALID_MEMBERSHIP_PERIOD(HttpStatus.BAD_REQUEST, "만료일은 시작일보다 빠를 수 없습니다."),

    // PT권
    PT_PASS_NOT_FOUND(HttpStatus.NOT_FOUND, "PT권을 찾을 수 없습니다."),
    INSUFFICIENT_PT_COUNT(HttpStatus.CONFLICT, "PT 잔여 횟수가 부족합니다."),
    PT_COUNT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PT 잔여 횟수가 허용 범위를 넘습니다."),

    // 트레이너
    TRAINER_NOT_FOUND(HttpStatus.NOT_FOUND, "트레이너를 찾을 수 없습니다."),
    TRAINER_INACTIVE(HttpStatus.CONFLICT, "비활성 트레이너에게는 예약할 수 없습니다."),

    // PT 예약
    PT_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PT 예약을 찾을 수 없습니다."),
    PT_RESERVATION_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 예약입니다."),
    INVALID_PT_RESERVATION_TIME(HttpStatus.BAD_REQUEST, "예약 시각은 영업 시간 안에서 10분 단위여야 합니다."),
    INVALID_PT_RESERVATION_PERIOD(HttpStatus.BAD_REQUEST, "조회 기간은 시작일부터 종료일까지 31일 이내여야 합니다."),
    PT_RESERVATION_IN_PAST(HttpStatus.BAD_REQUEST, "지난 시각에는 예약할 수 없습니다."),
    NO_PT_PASS_FOR_SESSION(HttpStatus.CONFLICT, "해당 수업 길이의 잔여 PT권이 없습니다."),
    TRAINER_TIME_CONFLICT(HttpStatus.CONFLICT, "해당 트레이너의 다른 예약과 시간이 겹칩니다."),
    MEMBER_TIME_CONFLICT(HttpStatus.CONFLICT, "해당 회원의 다른 예약과 시간이 겹칩니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
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
