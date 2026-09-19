package com.gym.domain.ptreservation.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.ptreservation.dto.PtReservationCreateRequest;
import com.gym.domain.ptreservation.dto.PtReservationResponse;
import com.gym.domain.ptreservation.service.PtReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pt-reservations")
public class PtReservationController {

    private final PtReservationService ptReservationService;

    public PtReservationController(PtReservationService ptReservationService) {
        this.ptReservationService = ptReservationService;
    }

    /** 시간표 조회. from·to 는 한국 날짜(yyyy-MM-dd)이고 둘 다 포함한다. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PtReservationResponse>>> getReservations(
            @RequestParam Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(ptReservationService.getReservations(trainerId, from, to)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PtReservationResponse>> create(
            @Valid @RequestBody PtReservationCreateRequest request, Authentication authentication) {
        PtReservationResponse response = ptReservationService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 취소는 예약을 지우는 것이 아니라 상태를 바꾸고 횟수를 돌려주는 행위라 DELETE 가 아니다
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<PtReservationResponse>> cancel(
            @PathVariable Long reservationId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                ptReservationService.cancel(reservationId, authentication.getName())));
    }
}
