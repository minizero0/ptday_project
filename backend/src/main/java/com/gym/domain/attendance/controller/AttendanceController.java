package com.gym.domain.attendance.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.attendance.dto.AttendanceCheckInRequest;
import com.gym.domain.attendance.dto.AttendanceListResponse;
import com.gym.domain.attendance.dto.CheckInResult;
import com.gym.domain.attendance.service.AttendanceService;
import com.gym.domain.attendance.service.CheckInChannel;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

    private static final String KIOSK_AUTHORITY = "ROLE_KIOSK";

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * 출석 체크. 기록이 생성되면 201, 뒷번호 중복으로 후보 선택이 필요하면 200 + 후보 목록.
     * 입구 키오스크 계정의 요청이거나, 직원 계정으로 띄운 출석 화면이 kiosk=true 로 알려 오면
     * 후보의 이름을 가려서 내려준다 — 그 화면을 보는 사람은 회원이기 때문이다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckInResult>> checkIn(
            @RequestBody AttendanceCheckInRequest request,
            @RequestParam(defaultValue = "false") boolean kiosk,
            Authentication authentication) {
        CheckInResult result = attendanceService.checkIn(request, resolveChannel(authentication, kiosk));
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

    // 키오스크 계정은 요청값과 상관없이 항상 가린다. 파라미터는 더 가리는 쪽으로만 쓸 수 있다.
    private CheckInChannel resolveChannel(Authentication authentication, boolean isKioskScreen) {
        boolean isKioskAccount = authentication.getAuthorities().stream()
                .anyMatch(authority -> KIOSK_AUTHORITY.equals(authority.getAuthority()));
        return isKioskAccount || isKioskScreen ? CheckInChannel.KIOSK : CheckInChannel.STAFF;
    }
}
