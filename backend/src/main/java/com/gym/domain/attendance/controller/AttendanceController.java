package com.gym.domain.attendance.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.attendance.dto.AttendanceCheckInRequest;
import com.gym.domain.attendance.dto.AttendanceListResponse;
import com.gym.domain.attendance.dto.CheckInResult;
import com.gym.domain.attendance.service.AttendanceService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /** 출석 체크. 기록이 생성되면 201, 뒷번호 중복으로 후보 선택이 필요하면 200 + 후보 목록. */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckInResult>> checkIn(
            @RequestBody AttendanceCheckInRequest request) {
        CheckInResult result = attendanceService.checkIn(request);
        HttpStatus status = result.status() == CheckInResult.Status.CHECKED_IN
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(result));
    }

    /** 날짜별 출석 현황. date 생략 시 오늘(한국 기준). */
    @GetMapping
    public ResponseEntity<ApiResponse<AttendanceListResponse>> getDailyAttendances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        AttendanceListResponse response = attendanceService.getDailyAttendances(date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
