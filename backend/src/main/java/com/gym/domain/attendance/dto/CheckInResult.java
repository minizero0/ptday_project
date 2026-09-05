package com.gym.domain.attendance.dto;

import com.gym.domain.member.entity.Member;
import java.util.List;

/**
 * 출석 체크 결과. 즉시 완료(CHECKED_IN) 또는
 * 연락처 뒷자리 중복으로 직원 선택이 필요한 상태(CANDIDATES) 두 가지다.
 */
public record CheckInResult(
        Status status, AttendanceResponse attendance, List<CheckInCandidate> candidates) {

    public enum Status {
        CHECKED_IN,
        CANDIDATES
    }

    public static CheckInResult checkedIn(AttendanceResponse attendance) {
        return new CheckInResult(Status.CHECKED_IN, attendance, null);
    }

    public static CheckInResult candidates(List<CheckInCandidate> candidates) {
        return new CheckInResult(Status.CANDIDATES, null, candidates);
    }

    /** 뒷자리 중복 시 직원이 고를 후보. 연락처는 마스킹해 노출을 최소화한다. */
    public record CheckInCandidate(Long memberId, String memberNo, String name, String maskedPhone) {

        private static final int VISIBLE_TAIL_LENGTH = 4;

        public static CheckInCandidate from(Member member) {
            return new CheckInCandidate(
                    member.getId(), member.getMemberNo(), member.getName(), mask(member.getPhone()));
        }

        // 예) 01012345678 / 010-1234-5678 → 010-****-5678
        private static String mask(String phone) {
            if (phone == null) {
                return null;
            }
            String digits = phone.replaceAll("\\D", "");
            if (digits.length() <= VISIBLE_TAIL_LENGTH) {
                return digits;
            }
            String tail = digits.substring(digits.length() - VISIBLE_TAIL_LENGTH);
            String head = digits.length() >= 10 ? digits.substring(0, 3) : "";
            return head + "-****-" + tail;
        }
    }
}
