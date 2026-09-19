package com.gym.domain.member.dto;

import com.gym.domain.member.entity.Member;
import com.gym.domain.membership.dto.MembershipSummaryResponse;
import com.gym.domain.membership.entity.Membership;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 회원 목록의 한 줄. 단건 응답(MemberResponse)과 달리 데스크에서 목록만 보고도
 * 이용권 상태를 알 수 있도록 대표 이용권 요약을 함께 담는다.
 * membership 은 이용권 이력이 없으면 null 이다.
 */
public record MemberListItemResponse(
        Long id,
        String memberNo,
        String name,
        String phone,
        String gender,
        LocalDate birthDate,
        Instant createdAt,
        MembershipSummaryResponse membership) {

    public static MemberListItemResponse from(Member member, Membership membership, LocalDate today) {
        return new MemberListItemResponse(
                member.getId(),
                member.getMemberNo(),
                member.getName(),
                member.getPhone(),
                member.getGender(),
                member.getBirthDate(),
                member.getCreatedAt(),
                membership != null ? MembershipSummaryResponse.from(membership, today) : null);
    }
}
