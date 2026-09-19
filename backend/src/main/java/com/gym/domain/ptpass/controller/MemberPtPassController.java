package com.gym.domain.ptpass.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.ptpass.dto.PtPassCreateRequest;
import com.gym.domain.ptpass.dto.PtPassResponse;
import com.gym.domain.ptpass.service.PtPassService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원에 딸린 PT권. 접근 권한은 SecurityConfig 의 /api/members/** 규칙(ADMIN, STAFF)을 그대로 따른다.
 */
@RestController
@RequestMapping("/api/members/{memberId}/pt-passes")
public class MemberPtPassController {

    private final PtPassService ptPassService;

    public MemberPtPassController(PtPassService ptPassService) {
        this.ptPassService = ptPassService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PtPassResponse>> grantPtPass(
            @PathVariable Long memberId, @Valid @RequestBody PtPassCreateRequest request) {
        PtPassResponse response = ptPassService.grant(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PtPassResponse>>> getPtPasses(@PathVariable Long memberId) {
        List<PtPassResponse> response = ptPassService.getPtPassesByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
