package com.gym.domain.ptpass.entity;

/**
 * 잔여 횟수가 바뀐 경로. DB 에는 문자열로 저장한다.
 * PT 예약 도메인이 생기면 예약 차감·예약 취소 복원이 추가된다.
 */
public enum PtPassAdjustmentType {
    MANUAL
}
