package com.gym.domain.ptreservation.entity;

/** 예약 상태. 취소된 예약도 지우지 않고 남긴다 — 횟수 복원 이력의 근거다. */
public enum PtReservationStatus {
    RESERVED,
    CANCELED
}
