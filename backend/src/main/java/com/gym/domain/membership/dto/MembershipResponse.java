package com.gym.domain.membership.dto;

import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.entity.MembershipStatus;
import com.gym.domain.membership.service.MembershipPeriod;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 이용권 1건 응답. active 는 저장된 값이 아니라 기준일(today)로 판단한 "이용중" 여부다.
 * FE 는 이 값만 보고 (이용중) 배지를 그린다 — 같은 판단을 화면에서 다시 계산하지 않는다 (CLAUDE.md §6).
 * daysRemaining 도 같은 이유로 서버가 계산한다. 기준일이 서버(Asia/Seoul)와 브라우저에서 어긋나면
 * "이용중인데 D-day 는 음수" 같은 모순이 생기기 때문이다.
 */
public record MembershipResponse(
        Long id,
        Long memberId,
        LocalDate startDate,
        LocalDate endDate,
        MembershipStatus status,
        boolean active,
        // 만료일까지 남은 일수. 이용중이 아니면 null (만료일 당일이면 0)
        Long daysRemaining,
        Instant createdAt) {

    public static MembershipResponse from(Membership membership, LocalDate today) {
        boolean active = MembershipPeriod.isActiveOn(membership, today);
        return new MembershipResponse(
                membership.getId(),
                membership.getMember().getId(),
                membership.getStartDate(),
                membership.getEndDate(),
                membership.getStatus(),
                active,
                active ? ChronoUnit.DAYS.between(today, membership.getEndDate()) : null,
                membership.getCreatedAt());
    }
}
