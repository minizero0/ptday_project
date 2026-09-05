package com.gym.domain.membership.entity;

/**
 * 기준일에서 본 이용권 기간 상태. 저장 값(MembershipStatus)이 아니라 날짜 비교로 정해진다.
 * 날짜 판단은 서버 기준일(Asia/Seoul)로만 해야 하므로 이 값을 응답에 담아 화면에 내려준다 (CLAUDE.md §6).
 */
public enum MembershipPeriodStatus {
    SCHEDULED, // 시작 전
    ACTIVE, // 이용중
    EXPIRED // 만료
}
