package com.gym.domain.account.entity;

/**
 * 로그인 계정의 권한 구분. DB 에는 문자열로 저장한다(@Enumerated(STRING)).
 */
public enum Role {
    ADMIN,
    STAFF,
    TRAINER,
    MEMBER,
    // 입구 출석 키오스크(태블릿) 전용. 출석 체크만 할 수 있다
    KIOSK
}
