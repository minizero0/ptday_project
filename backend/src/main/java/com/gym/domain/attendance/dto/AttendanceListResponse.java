package com.gym.domain.attendance.dto;

import java.time.LocalDate;
import java.util.List;

/** 날짜별 출석 현황 응답. totalCount 가 화면의 "출석 N명" 값이다. */
public record AttendanceListResponse(LocalDate date, int totalCount, List<AttendanceResponse> items) {

    public static AttendanceListResponse of(LocalDate date, List<AttendanceResponse> items) {
        return new AttendanceListResponse(date, items.size(), items);
    }
}
