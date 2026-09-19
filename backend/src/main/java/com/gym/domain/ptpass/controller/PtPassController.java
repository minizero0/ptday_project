package com.gym.domain.ptpass.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.ptpass.dto.PtPassAdjustRequest;
import com.gym.domain.ptpass.dto.PtPassAdjustmentResponse;
import com.gym.domain.ptpass.dto.PtPassResponse;
import com.gym.domain.ptpass.service.PtPassService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pt-passes")
public class PtPassController {

    private final PtPassService ptPassService;

    public PtPassController(PtPassService ptPassService) {
        this.ptPassService = ptPassService;
    }

    /** 횟수 수동 조정. 처리자는 요청 본문이 아니라 인증 정보에서 가져온다 — 남의 이름으로 기록할 수 없다. */
    @PostMapping("/{ptPassId}/adjustments")
    public ResponseEntity<ApiResponse<PtPassResponse>> adjustPtPass(
            @PathVariable Long ptPassId,
            @Valid @RequestBody PtPassAdjustRequest request,
            Authentication authentication) {
        PtPassResponse response = ptPassService.adjust(ptPassId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{ptPassId}/adjustments")
    public ResponseEntity<ApiResponse<List<PtPassAdjustmentResponse>>> getAdjustments(
            @PathVariable Long ptPassId) {
        List<PtPassAdjustmentResponse> response = ptPassService.getAdjustments(ptPassId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
