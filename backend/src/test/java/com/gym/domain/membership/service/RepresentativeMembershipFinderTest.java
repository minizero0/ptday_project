package com.gym.domain.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gym.domain.member.entity.Member;
import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.repository.MembershipRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RepresentativeMembershipFinderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private RepresentativeMembershipFinder finder;

    @Test
    void 이용권이_하나도_없으면_대표도_없다() {
        assertThat(RepresentativeMembershipFinder.pick(List.of(), TODAY)).isEmpty();
    }

    @Test
    void 이용중인_이용권이_예정이나_만료보다_우선한다() {
        Member member = member(1L);
        Membership expired = membership(member, TODAY.minusMonths(3), TODAY.minusMonths(2));
        Membership active = membership(member, TODAY.minusDays(10), TODAY.plusDays(20));
        Membership scheduled = membership(member, TODAY.plusDays(30), TODAY.plusDays(60));

        assertThat(RepresentativeMembershipFinder.pick(List.of(expired, scheduled, active), TODAY))
                .containsSame(active);
    }

    @Test
    void 이용중인_것이_겹치면_가장_늦게_끝나는_것을_고른다() {
        // 갱신 구매로 유효 기간이 겹친 경우
        Member member = member(1L);
        Membership endsSooner = membership(member, TODAY.minusDays(20), TODAY.plusDays(5));
        Membership endsLater = membership(member, TODAY.minusDays(1), TODAY.plusDays(40));

        assertThat(RepresentativeMembershipFinder.pick(List.of(endsSooner, endsLater), TODAY))
                .containsSame(endsLater);
    }

    @Test
    void 시작일과_만료일_당일은_이용중이다() {
        Member member = member(1L);
        Membership startsToday = membership(member, TODAY, TODAY.plusDays(30));
        Membership endsToday = membership(member, TODAY.minusDays(30), TODAY);

        assertThat(RepresentativeMembershipFinder.pick(List.of(startsToday), TODAY)).containsSame(startsToday);
        assertThat(RepresentativeMembershipFinder.pick(List.of(endsToday), TODAY)).containsSame(endsToday);
    }

    @Test
    void 이용중인_것이_없으면_가장_빨리_시작하는_예정_이용권을_고른다() {
        Member member = member(1L);
        Membership expired = membership(member, TODAY.minusMonths(3), TODAY.minusMonths(2));
        Membership startsSooner = membership(member, TODAY.plusDays(3), TODAY.plusDays(33));
        Membership startsLater = membership(member, TODAY.plusDays(40), TODAY.plusDays(70));

        assertThat(RepresentativeMembershipFinder.pick(List.of(expired, startsLater, startsSooner), TODAY))
                .containsSame(startsSooner);
    }

    @Test
    void 만료된_것만_있으면_가장_최근에_끝난_것을_고른다() {
        Member member = member(1L);
        Membership endedLongAgo = membership(member, TODAY.minusMonths(12), TODAY.minusMonths(11));
        Membership endedRecently = membership(member, TODAY.minusMonths(2), TODAY.minusDays(1));

        assertThat(RepresentativeMembershipFinder.pick(List.of(endedLongAgo, endedRecently), TODAY))
                .containsSame(endedRecently);
    }

    @Test
    void 기간이_남았어도_강제_만료된_이용권은_이용중으로_보지_않는다() {
        Member member = member(1L);
        Membership forcedExpired = membership(member, TODAY.minusDays(5), TODAY.plusDays(25));
        forcedExpired.expire();
        Membership scheduled = membership(member, TODAY.plusDays(10), TODAY.plusDays(40));

        assertThat(RepresentativeMembershipFinder.pick(List.of(forcedExpired, scheduled), TODAY))
                .containsSame(scheduled);
    }

    @Test
    void 여러_회원의_대표_이용권을_한_번의_조회로_회원별로_고른다() {
        Member first = member(1L);
        Member second = member(2L);
        Membership firstExpired = membership(first, TODAY.minusMonths(3), TODAY.minusMonths(2));
        Membership firstActive = membership(first, TODAY.minusDays(10), TODAY.plusDays(20));
        Membership secondScheduled = membership(second, TODAY.plusDays(5), TODAY.plusDays(35));
        Set<Long> memberIds = Set.of(1L, 2L, 3L);
        when(membershipRepository.findByMemberIdInOrderByStartDateDescIdDesc(memberIds))
                .thenReturn(List.of(firstActive, secondScheduled, firstExpired));

        Map<Long, Membership> result = finder.findByMemberIds(memberIds, TODAY);

        // 이용권 이력이 없는 3번 회원은 결과에 없다
        assertThat(result).containsOnlyKeys(1L, 2L);
        assertThat(result.get(1L)).isSameAs(firstActive);
        assertThat(result.get(2L)).isSameAs(secondScheduled);
    }

    @Test
    void 대상_회원이_없으면_조회하지_않는다() {
        assertThat(finder.findByMemberIds(Set.of(), TODAY)).isEmpty();
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void 회원_한_명의_대표_이용권을_고른다() {
        Member member = member(1L);
        Membership active = membership(member, TODAY.minusDays(10), TODAY.plusDays(20));
        when(membershipRepository.findByMemberIdOrderByStartDateDescIdDesc(1L)).thenReturn(List.of(active));

        assertThat(finder.findByMemberId(1L, TODAY)).containsSame(active);
    }

    private static Member member(long id) {
        Member member = new Member("26090000" + id, "회원" + id, null, null, null);
        // id 는 DB 가 채우는 값이라 세터가 없다. 회원별로 묶는 동작을 보려면 테스트에서만 직접 넣는다.
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private static Membership membership(Member member, LocalDate startDate, LocalDate endDate) {
        return new Membership(member, startDate, endDate);
    }
}
