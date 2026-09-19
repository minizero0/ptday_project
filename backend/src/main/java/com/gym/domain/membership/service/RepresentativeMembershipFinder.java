package com.gym.domain.membership.service;

import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.repository.MembershipRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 회원별 "대표 이용권" 1건을 고른다.
 * 출석 현황과 회원 목록처럼 회원 한 줄에 이용권 상태 하나만 보여주는 화면들이
 * 서로 다른 기준을 쓰지 않도록 고르는 규칙을 여기 한곳에 둔다.
 */
@Component
public class RepresentativeMembershipFinder {

    private final MembershipRepository membershipRepository;

    public RepresentativeMembershipFinder(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Optional<Membership> findByMemberId(Long memberId, LocalDate today) {
        return pick(membershipRepository.findByMemberIdOrderByStartDateDescIdDesc(memberId), today);
    }

    /**
     * 여러 회원의 대표 이용권을 한 번의 조회로 가져온다(회원마다 조회하면 N+1).
     * 이용권 이력이 없는 회원은 결과에 담기지 않는다.
     */
    public Map<Long, Membership> findByMemberIds(Collection<Long> memberIds, LocalDate today) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Membership>> membershipsByMemberId =
                membershipRepository.findByMemberIdInOrderByStartDateDescIdDesc(memberIds).stream()
                        .collect(Collectors.groupingBy(membership -> membership.getMember().getId()));

        return membershipsByMemberId.entrySet().stream()
                .flatMap(entry -> pick(entry.getValue(), today)
                        .map(membership -> Map.entry(entry.getKey(), membership))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 이용중(만료일이 가장 늦은 것) > 예정(가장 빨리 시작하는 것) > 만료(가장 최근에 끝난 것) 순.
     * 갱신 구매로 기간이 겹치거나 선등록 건이 섞여 있어도 "지금 상태"가 먼저 보이게 하기 위한 순서다.
     */
    static Optional<Membership> pick(List<Membership> memberships, LocalDate today) {
        return memberships.stream()
                .filter(membership -> MembershipPeriod.isActiveOn(membership, today))
                .max(Comparator.comparing(Membership::getEndDate))
                .or(() -> memberships.stream()
                        .filter(membership -> today.isBefore(membership.getStartDate()))
                        .min(Comparator.comparing(Membership::getStartDate)))
                .or(() -> memberships.stream().max(Comparator.comparing(Membership::getEndDate)));
    }
}
