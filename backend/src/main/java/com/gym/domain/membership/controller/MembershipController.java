package com.gym.domain.membership.controller;

import com.gym.common.response.ApiResponse;
import com.gym.common.response.PageResponse;
import com.gym.domain.membership.dto.MembershipListItemResponse;
import com.gym.domain.membership.dto.MembershipResponse;
import com.gym.domain.membership.dto.MembershipUpdateRequest;
import com.gym.domain.membership.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이용권 관리 화면용 전체 목록. 회원별 부여·이력 조회는 MemberMembershipController 가 맡는다.
 */
@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MembershipListItemResponse>>> getMemberships(
            @RequestParam(defaultValue = "false") boolean includeExpired, Pageable pageable) {
        PageResponse<MembershipListItemResponse> response =
                membershipService.getMemberships(includeExpired, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{membershipId}")
    public ResponseEntity<ApiResponse<MembershipResponse>> updateMembership(
            @PathVariable Long membershipId, @Valid @RequestBody MembershipUpdateRequest request) {
        MembershipResponse response = membershipService.updatePeriod(membershipId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
