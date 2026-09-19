package com.gym.domain.ptpass.entity;

/** 잔여 횟수가 바뀐 경로. DB 에는 문자열로 저장한다 (pt_pass_adjustment_type_check 와 같은 값). */
public enum PtPassAdjustmentType {
    // 직원의 수동 조정
    MANUAL,
    // PT 예약에 따른 자동 차감
    RESERVATION,
    // PT 예약 취소에 따른 자동 복원
    RESERVATION_CANCEL
}
