package com.gym.domain.attendance.service;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.attendance.dto.AttendanceCheckInRequest;
import com.gym.domain.attendance.dto.AttendanceListResponse;
import com.gym.domain.attendance.dto.AttendanceResponse;
import com.gym.domain.attendance.dto.CheckInResult;
import com.gym.domain.attendance.dto.CheckInResult.CheckInCandidate;
import com.gym.domain.attendance.entity.Attendance;
import com.gym.domain.attendance.repository.AttendanceRepository;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.entity.MembershipPeriodStatus;
import com.gym.domain.membership.service.MembershipPeriod;
import com.gym.domain.membership.service.RepresentativeMembershipFinder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    // 출석 체크에 쓰는 연락처 뒷자리 수 (고정)
    private static final int PHONE_LAST_DIGITS_LENGTH = 4;
    // "날짜별 출석"의 하루 기준은 헬스장 영업 시간대(한국). API 전송은 UTC Instant 로 한다 (CLAUDE.md §5)
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final RepresentativeMembershipFinder representativeMembershipFinder;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            MemberRepository memberRepository,
            RepresentativeMembershipFinder representativeMembershipFinder) {
        this.attendanceRepository = attendanceRepository;
        this.memberRepository = memberRepository;
        this.representativeMembershipFinder = representativeMembershipFinder;
    }

    /**
     * 출석 체크. 요청의 세 필드(memberId / memberNo / phoneLastDigits) 중 정확히 하나만 사용한다.
     * 연락처 뒷자리가 여러 회원과 일치하면 저장하지 않고 후보 목록을 돌려준다.
     * 오늘 이용 중인 이용권이 없는 회원은 출석시키지 않는다.
     */
    @Transactional
    public CheckInResult checkIn(AttendanceCheckInRequest request, CheckInChannel channel) {
        validateExactlyOneKey(request);

        if (request.memberId() != null) {
            Member member = memberRepository
                    .findByIdAndDeletedAtIsNull(request.memberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            return CheckInResult.checkedIn(saveAttendance(member));
        }

        if (hasText(request.memberNo())) {
            Member member = memberRepository
                    .findByMemberNoAndDeletedAtIsNull(request.memberNo().trim())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            return CheckInResult.checkedIn(saveAttendance(member));
        }

        return checkInByPhoneLastDigits(request.phoneLastDigits().trim(), channel);
    }

    @Transactional(readOnly = true)
    public AttendanceListResponse getDailyAttendances(LocalDate requestedDate) {
        LocalDate date = requestedDate != null ? requestedDate : LocalDate.now(BUSINESS_ZONE);
        Instant start = date.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        List<Attendance> attendances = attendanceRepository.findAllWithMemberBetween(start, end);

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Set<Long> memberIds = attendances.stream()
                .map(attendance -> attendance.getMember().getId())
                .collect(Collectors.toSet());
        Map<Long, Membership> membershipByMemberId =
                representativeMembershipFinder.findByMemberIds(memberIds, today);

        List<AttendanceResponse> items = attendances.stream()
                .map(attendance -> AttendanceResponse.from(
                        attendance, membershipByMemberId.get(attendance.getMember().getId()), today))
                .toList();
        return AttendanceListResponse.of(date, items);
    }

    private CheckInResult checkInByPhoneLastDigits(String digits, CheckInChannel channel) {
        if (!digits.matches("\\d{" + PHONE_LAST_DIGITS_LENGTH + "}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        List<Member> matched = memberRepository.findActiveByPhoneEndingWith(digits);
        if (matched.isEmpty()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (matched.size() == 1) {
            return CheckInResult.checkedIn(saveAttendance(matched.get(0)));
        }
        boolean shouldMaskName = channel == CheckInChannel.KIOSK;
        return CheckInResult.candidates(matched.stream()
                .map(member -> CheckInCandidate.from(member, shouldMaskName))
                .toList());
    }

    private AttendanceResponse saveAttendance(Member member) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        // 대표 이용권은 이용중 > 예정 > 만료 순으로 고른다 — 이용중이 아니면 오늘 쓸 수 있는 이용권이 없다는 뜻이다
        Membership membership = representativeMembershipFinder.findByMemberId(member.getId(), today)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_ACTIVE));
        MembershipPeriodStatus status = MembershipPeriod.statusOn(membership, today);
        if (status == MembershipPeriodStatus.EXPIRED) {
            // 가장 흔한 거절 사유라 따로 알려준다 — 회원이 재등록이 필요하다는 것을 바로 알 수 있게
            throw new BusinessException(ErrorCode.MEMBERSHIP_EXPIRED);
        }
        if (status != MembershipPeriodStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_ACTIVE);
        }

        Attendance saved = attendanceRepository.save(new Attendance(member));
        return AttendanceResponse.from(saved, membership, today);
    }

    private void validateExactlyOneKey(AttendanceCheckInRequest request) {
        int keyCount = 0;
        if (request.memberId() != null) {
            keyCount++;
        }
        if (hasText(request.memberNo())) {
            keyCount++;
        }
        if (hasText(request.phoneLastDigits())) {
            keyCount++;
        }
        if (keyCount != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
