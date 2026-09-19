package com.gym.domain.membership.dto;

import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.entity.MembershipPeriodStatus;
import com.gym.domain.membership.service.MembershipPeriod;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 회원 한 줄에 붙여 보여주는 이용권 요약. 상태와 남은 일수는 서버가 기준일로 판단해 내려준다 —
 * 화면에서 다시 계산하지 않는다 (CLAUDE.md §6).
 */
public record MembershipSummaryResponse(
        Long id,
        MembershipPeriodStatus status,
        LocalDate startDate,
        LocalDate endDate,
        // 만료까지 남은 일수. 이용중이 아니면 null (만료일 당일이면 0)
        Long daysRemaining) {

    public static MembershipSummaryResponse from(Membership membership, LocalDate today) {
        MembershipPeriodStatus status = MembershipPeriod.statusOn(membership, today);
        return new MembershipSummaryResponse(
                membership.getId(),
                status,
                membership.getStartDate(),
                membership.getEndDate(),
                status == MembershipPeriodStatus.ACTIVE
                        ? ChronoUnit.DAYS.between(today, membership.getEndDate())
                        : null);
    }
}
