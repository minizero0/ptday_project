package com.gym.domain.attendance.dto;

import com.gym.domain.member.entity.Member;
import java.util.List;

/**
 * 출석 체크 결과. 즉시 완료(CHECKED_IN) 또는
 * 연락처 뒷자리 중복으로 후보 선택이 필요한 상태(CANDIDATES) 두 가지다.
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

    /**
     * 뒷자리 중복 시 고를 후보. 연락처는 마스킹해 노출을 최소화한다.
     * 키오스크처럼 회원이 직접 보는 화면에는 이름도 가려서 내려준다(같은 뒷자리를 가진 남의 이름이 보이지 않게).
     */
    public record CheckInCandidate(Long memberId, String memberNo, String name, String maskedPhone) {

        private static final int VISIBLE_TAIL_LENGTH = 4;

        public static CheckInCandidate from(Member member, boolean shouldMaskName) {
            String name = shouldMaskName ? maskName(member.getName()) : member.getName();
            return new CheckInCandidate(member.getId(), member.getMemberNo(), name, mask(member.getPhone()));
        }

        // 예) 홍길동 → 홍*동, 남궁민수 → 남**수, 이준 → 이*
        private static String maskName(String name) {
            if (name.length() <= 1) {
                return name;
            }
            if (name.length() == 2) {
                return name.charAt(0) + "*";
            }
            return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
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
