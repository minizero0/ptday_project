package com.gym.domain.ptpass.dto;

import com.gym.domain.ptpass.entity.PtPass;
import java.time.Instant;

public record PtPassResponse(
        Long id,
        Long memberId,
        int totalCount,
        int remainingCount,
        Instant createdAt) {

    public static PtPassResponse from(PtPass ptPass) {
        return new PtPassResponse(
                ptPass.getId(),
                ptPass.getMember().getId(),
                ptPass.getTotalCount(),
                ptPass.getRemainingCount(),
                ptPass.getCreatedAt());
    }
}
