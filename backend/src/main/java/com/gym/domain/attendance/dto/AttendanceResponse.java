package com.gym.domain.attendance.dto;

import com.gym.domain.attendance.entity.Attendance;
import java.time.Instant;

/** 출석 1건 응답. 그리드에 필요한 회원 번호/이름을 함께 담는다. */
public record AttendanceResponse(
        Long id, Long memberId, String memberNo, String name, Instant checkedInAt) {

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getMember().getId(),
                attendance.getMember().getMemberNo(),
                attendance.getMember().getName(),
                attendance.getCheckedInAt());
    }
}
