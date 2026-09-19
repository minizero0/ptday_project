package com.gym.domain.ptreservation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gym.domain.ptpass.entity.PtPass;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * 예약 요청. 어느 PT권에서 차감할지는 받지 않는다 — 수업 길이만 고르면 서버가 먼저 산 PT권부터 쓴다.
 * 종료 시각도 받지 않는다(시작 + 수업 길이). 수업 길이·시각 규칙의 검증은 도메인이 한다.
 */
public record PtReservationCreateRequest(
        @NotNull(message = "회원을 선택하세요.")
        Long memberId,

        @NotNull(message = "트레이너를 선택하세요.")
        Long trainerId,

        @NotNull(message = "수업 길이를 선택하세요.")
        Integer sessionMinutes,

        @NotNull(message = "예약 시각을 선택하세요.")
        Instant startTime) {

    @JsonIgnore
    @AssertTrue(message = "수업 길이는 30, 40, 50, 60분 중 하나여야 합니다.")
    public boolean isSessionMinutesAllowed() {
        // null 은 @NotNull 이 따로 알려준다
        return sessionMinutes == null || PtPass.ALLOWED_SESSION_MINUTES.contains(sessionMinutes);
    }
}
