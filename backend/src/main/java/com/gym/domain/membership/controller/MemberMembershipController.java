package com.gym.domain.membership.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.membership.dto.MembershipCreateRequest;
import com.gym.domain.membership.dto.MembershipResponse;
import com.gym.domain.membership.service.MembershipService;
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
 * 이용권은 회원에 종속된 자원이므로 회원 경로 아래에 둔다 (CLAUDE.md §5).
 * 접근 권한은 SecurityConfig 의 /api/members/** 규칙(ADMIN, STAFF)을 그대로 따른다.
 */
@RestController
@RequestMapping("/api/members/{memberId}/memberships")
public class MemberMembershipController {

    private final MembershipService membershipService;

    public MemberMembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MembershipResponse>> grantMembership(
            @PathVariable Long memberId, @Valid @RequestBody MembershipCreateRequest request) {
        MembershipResponse response = membershipService.grant(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMemberships(
            @PathVariable Long memberId) {
        List<MembershipResponse> response = membershipService.getMembershipsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
