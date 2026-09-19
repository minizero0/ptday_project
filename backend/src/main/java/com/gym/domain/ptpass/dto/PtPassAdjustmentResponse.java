package com.gym.domain.ptpass.dto;

import com.gym.domain.ptpass.entity.PtPassAdjustment;
import com.gym.domain.ptpass.entity.PtPassAdjustmentType;
import java.time.Instant;

public record PtPassAdjustmentResponse(
        Long id,
        PtPassAdjustmentType type,
        int delta,
        int remainingAfter,
        String reason,
        String adjustedBy,
        Instant createdAt) {

    public static PtPassAdjustmentResponse from(PtPassAdjustment adjustment) {
        return new PtPassAdjustmentResponse(
                adjustment.getId(),
                adjustment.getType(),
                adjustment.getDelta(),
                adjustment.getRemainingAfter(),
                adjustment.getReason(),
                adjustment.getAdjustedBy(),
                adjustment.getCreatedAt());
    }
}
