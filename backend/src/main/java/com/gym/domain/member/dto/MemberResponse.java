package com.gym.domain.member.dto;

import com.gym.domain.member.entity.Member;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 회원 응답. 엔티티를 외부로 직접 노출하지 않기 위한 변환 결과 (CLAUDE.md §3.3).
 */
public record MemberResponse(
        Long id,
        String memberNo,
        String name,
        String phone,
        String gender,
        LocalDate birthDate,
        Instant createdAt) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getMemberNo(),
                member.getName(),
                member.getPhone(),
                member.getGender(),
                member.getBirthDate(),
                member.getCreatedAt());
    }
}
