package com.gym.domain.membership.dto;

import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.entity.MembershipPeriodStatus;
import com.gym.domain.membership.service.MembershipPeriod;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 이용권 관리 목록의 한 줄. 회원별 상세와 달리 어느 회원 것인지 함께 보여줘야 하므로
 * 회원번호·이름을 같이 담는다.
 */
public record MembershipListItemResponse(
        Long id,
        Long memberId,
        String memberNo,
        String memberName,
        LocalDate startDate,
        LocalDate endDate,
        MembershipPeriodStatus status,
        // 만료까지 남은 일수. 이용중이 아니면 null (만료일 당일이면 0)
        Long daysRemaining) {

    public static MembershipListItemResponse from(Membership membership, LocalDate today) {
        MembershipPeriodStatus status = MembershipPeriod.statusOn(membership, today);
        return new MembershipListItemResponse(
                membership.getId(),
                membership.getMember().getId(),
                membership.getMember().getMemberNo(),
                membership.getMember().getName(),
                membership.getStartDate(),
                membership.getEndDate(),
                status,
                status == MembershipPeriodStatus.ACTIVE
                        ? ChronoUnit.DAYS.between(today, membership.getEndDate())
                        : null);
    }
}
