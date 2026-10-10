package com.gym.domain.ptpass.dto;

import com.gym.domain.ptpass.entity.PtPass;
import java.time.Instant;

/**
 * PT권 관리 목록의 한 줄. 회원별 목록과 달리 어느 회원 것인지 함께 보여줘야 하므로
 * 회원번호·이름을 같이 담는다.
 */
public record PtPassListItemResponse(
        Long id,
        Long memberId,
        String memberNo,
        String memberName,
        int sessionMinutes,
        int totalCount,
        int remainingCount,
        Instant createdAt) {
    public static PtPassListItemResponse from(PtPass ptPass) {
        return new PtPassListItemResponse(
                ptPass.getId(),
                ptPass.getMember().getId(),
                ptPass.getMember().getMemberNo(),
                ptPass.getMember().getName(),
                ptPass.getSessionMinutes(),
                ptPass.getTotalCount(),
                ptPass.getRemainingCount(),
                ptPass.getCreatedAt());
    }
}
