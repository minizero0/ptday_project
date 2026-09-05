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
import com.gym.domain.membership.repository.MembershipRepository;
import com.gym.domain.membership.service.MembershipPeriod;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
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
    private final MembershipRepository membershipRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            MemberRepository memberRepository,
            MembershipRepository membershipRepository) {
        this.attendanceRepository = attendanceRepository;
        this.memberRepository = memberRepository;
        this.membershipRepository = membershipRepository;
    }

    /**
     * 출석 체크. 요청의 세 필드(memberId / memberNo / phoneLastDigits) 중 정확히 하나만 사용한다.
     * 연락처 뒷자리가 여러 회원과 일치하면 저장하지 않고 후보 목록을 돌려준다.
     */
    @Transactional
    public CheckInResult checkIn(AttendanceCheckInRequest request) {
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

        return checkInByPhoneLastDigits(request.phoneLastDigits().trim());
    }

    @Transactional(readOnly = true)
    public AttendanceListResponse getDailyAttendances(LocalDate requestedDate) {
        LocalDate date = requestedDate != null ? requestedDate : LocalDate.now(BUSINESS_ZONE);
        Instant start = date.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        List<Attendance> attendances = attendanceRepository.findAllWithMemberBetween(start, end);

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Map<Long, Membership> membershipByMemberId = loadRepresentativeMemberships(attendances, today);

        List<AttendanceResponse> items = attendances.stream()
                .map(attendance -> AttendanceResponse.from(
                        attendance, membershipByMemberId.get(attendance.getMember().getId()), today))
                .toList();
        return AttendanceListResponse.of(date, items);
    }

    private CheckInResult checkInByPhoneLastDigits(String digits) {
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
        return CheckInResult.candidates(matched.stream().map(CheckInCandidate::from).toList());
    }

    private AttendanceResponse saveAttendance(Member member) {
        Attendance saved = attendanceRepository.save(new Attendance(member));
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Membership membership = pickRepresentative(
                membershipRepository.findByMemberIdOrderByStartDateDescIdDesc(member.getId()), today);
        return AttendanceResponse.from(saved, membership, today);
    }

    /** 출석한 회원들의 이용권을 한 번의 조회로 가져와 회원별 대표 1건으로 줄인다(N+1 방지). */
    private Map<Long, Membership> loadRepresentativeMemberships(
            List<Attendance> attendances, LocalDate today) {
        Set<Long> memberIds = attendances.stream()
                .map(attendance -> attendance.getMember().getId())
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return membershipRepository.findByMemberIdInOrderByStartDateDescIdDesc(memberIds).stream()
                .collect(Collectors.groupingBy(membership -> membership.getMember().getId()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> pickRepresentative(entry.getValue(), today)));
    }

    /**
     * 그리드에 보여줄 대표 이용권을 고른다.
     * 이용중(만료일이 가장 늦은 것) > 예정(가장 빨리 시작하는 것) > 만료(가장 최근 것) 순.
     * 갱신 구매로 기간이 겹치거나 선등록 건이 섞여 있어도 "지금 상태"가 먼저 보이게 하기 위한 순서다.
     */
    private Membership pickRepresentative(List<Membership> memberships, LocalDate today) {
        if (memberships.isEmpty()) {
            return null;
        }

        return memberships.stream()
                .filter(membership -> MembershipPeriod.isActiveOn(membership, today))
                .max(Comparator.comparing(Membership::getEndDate))
                .or(() -> memberships.stream()
                        .filter(membership -> today.isBefore(membership.getStartDate()))
                        .min(Comparator.comparing(Membership::getStartDate)))
                .or(() -> memberships.stream().max(Comparator.comparing(Membership::getEndDate)))
                .orElse(null);
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
