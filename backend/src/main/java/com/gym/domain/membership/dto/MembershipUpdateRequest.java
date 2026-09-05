package com.gym.domain.membership.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 이용권 수정 요청. 시작일과 만료일은 서로 얽혀 있어 함께 받는다.
 * 보유 회원은 바꿀 수 없다 — 다른 회원의 것이라면 정정이 아니라 잘못 등록한 건이다.
 */
public record MembershipUpdateRequest(
        @NotNull(message = "시작일을 입력하세요.")
        LocalDate startDate,

        @NotNull(message = "만료일을 입력하세요.")
        LocalDate endDate) {
}
