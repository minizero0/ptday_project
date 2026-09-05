package com.gym.domain.membership.service;

import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.entity.MembershipPeriodStatus;
import com.gym.domain.membership.entity.MembershipStatus;
import java.time.LocalDate;

/**
 * 이용권 기간 판단의 단일 출처 (CLAUDE.md §9 — 만료 판단은 Service 계층에 둔다).
 * 상세 패널과 출석 그리드가 같은 기준을 쓰도록 여기 한 곳에서만 계산한다.
 */
public final class MembershipPeriod {

    private MembershipPeriod() {
        // 유틸 클래스 — 인스턴스화 금지
    }

    /** 기준일에서 본 기간 상태. 만료일 당일까지는 이용 가능하다 (docs/ERD.md §3.5). */
    public static MembershipPeriodStatus statusOn(Membership membership, LocalDate today) {
        if (membership.getStatus() == MembershipStatus.EXPIRED) {
            return MembershipPeriodStatus.EXPIRED;
        }
        if (today.isBefore(membership.getStartDate())) {
            return MembershipPeriodStatus.SCHEDULED;
        }
        if (today.isAfter(membership.getEndDate())) {
            return MembershipPeriodStatus.EXPIRED;
        }
        return MembershipPeriodStatus.ACTIVE;
    }

    public static boolean isActiveOn(Membership membership, LocalDate today) {
        return statusOn(membership, today) == MembershipPeriodStatus.ACTIVE;
    }
}
