package com.gym.domain.attendance.service;

import java.time.LocalDate;

/**
 * 출석이 기록됐음을 알리는 애플리케이션 이벤트.
 *
 * @param date 출석이 기록된 날짜(영업 시간대 기준)
 */
public record AttendanceCheckedInEvent(LocalDate date) {
}
