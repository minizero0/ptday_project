package com.gym.domain.attendance.dto;

import com.gym.domain.attendance.entity.Attendance;
import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.entity.MembershipPeriodStatus;
import com.gym.domain.membership.service.MembershipPeriod;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 출석 1건 응답. 그리드에 필요한 회원 번호/이름과 이용권 요약을 함께 담는다.
 * membership 은 보유 이력이 없으면 null 이다.
 */
public record AttendanceResponse(
        Long id,
        Long memberId,
        String memberNo,
        String name,
        Instant checkedInAt,
        MembershipSummary membership) {

    /** 그리드 한 줄에 표시할 이용권 요약. 상태는 서버 기준일로 판단해 내려준다 (CLAUDE.md §6). */
    public record MembershipSummary(
            MembershipPeriodStatus status,
            LocalDate startDate,
            LocalDate endDate,
            // 만료까지 남은 일수. 이용중이 아니면 null (만료일 당일이면 0). 만료 임박 안내에 쓴다
            Long daysRemaining) {

        public static MembershipSummary from(Membership membership, LocalDate today) {
            MembershipPeriodStatus status = MembershipPeriod.statusOn(membership, today);
            return new MembershipSummary(
                    status,
                    membership.getStartDate(),
                    membership.getEndDate(),
                    status == MembershipPeriodStatus.ACTIVE
                            ? ChronoUnit.DAYS.between(today, membership.getEndDate())
                            : null);
        }
    }

    public static AttendanceResponse from(
            Attendance attendance, Membership membership, LocalDate today) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getMember().getId(),
                attendance.getMember().getMemberNo(),
                attendance.getMember().getName(),
                attendance.getCheckedInAt(),
                membership != null ? MembershipSummary.from(membership, today) : null);
    }
}
