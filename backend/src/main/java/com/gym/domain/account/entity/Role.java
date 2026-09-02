package com.gym.domain.account.entity;

/**
 * 로그인 계정의 권한 구분. DB 에는 문자열로 저장한다(@Enumerated(STRING)).
 */
public enum Role {
    ADMIN,
    STAFF,
    TRAINER,
    MEMBER
}
