package com.gym.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gym.common.response.PageResponse;
import com.gym.common.util.BusinessTime;
import com.gym.domain.member.dto.MemberCreateRequest;
import com.gym.domain.member.dto.MemberListItemResponse;
import com.gym.domain.member.dto.MemberResponse;
import com.gym.domain.member.dto.MemberUpdateRequest;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.entity.MembershipPeriodStatus;
import com.gym.domain.membership.service.RepresentativeMembershipFinder;
import com.gym.domain.ptpass.service.PtRemainingCountFinder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);
    private static final long MEMBER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RepresentativeMembershipFinder representativeMembershipFinder;

    @Mock
    private PtRemainingCountFinder ptRemainingCountFinder;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 회원을_등록하면_전화번호를_하이픈_표기로_통일해_저장한다() {
        // Arrange
        when(memberRepository.findTopByMemberNoStartingWithOrderByMemberNoDesc(anyString()))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MemberCreateRequest request = new MemberCreateRequest("홍길동", "01012345678", "남", BIRTH_DATE);

        // Act
        MemberResponse response = memberService.createMember(request);

        // Assert
        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(saved.capture());
        assertThat(saved.getValue().getPhone()).isEqualTo("010-1234-5678");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
    }

    @Test
    void 전화번호를_비워_등록하면_null_로_저장한다() {
        when(memberRepository.findTopByMemberNoStartingWithOrderByMemberNoDesc(anyString()))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MemberCreateRequest request = new MemberCreateRequest("홍길동", "", null, null);

        MemberResponse response = memberService.createMember(request);

        assertThat(response.phone()).isNull();
    }

    @Test
    void 회원_정보를_수정하면_전화번호를_하이픈_표기로_통일한다() {
        Member member = new Member("260900001", "홍길동", "010-1111-2222", "남", BIRTH_DATE);
        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.of(member));
        MemberUpdateRequest request = new MemberUpdateRequest("홍길동", "021234567", "남", BIRTH_DATE);

        MemberResponse response = memberService.updateMember(MEMBER_ID, request);

        assertThat(member.getPhone()).isEqualTo("02-123-4567");
        assertThat(response.phone()).isEqualTo("02-123-4567");
    }

    @Test
    void 회원_목록의_각_줄에_대표_이용권_요약을_담는다() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Member withMembership = member(1L, "김철수");
        Member withoutMembership = member(2L, "이영희");
        when(memberRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(withMembership, withoutMembership), pageable, 2));
        LocalDate today = BusinessTime.today();
        Membership active = new Membership(withMembership, today.minusDays(10), today.plusDays(20));
        when(representativeMembershipFinder.findByMemberIds(eq(Set.of(1L, 2L)), eq(today)))
                .thenReturn(Map.of(1L, active));
        when(ptRemainingCountFinder.findByMemberIds(Set.of(1L, 2L))).thenReturn(Map.of(1L, 12));

        // Act
        PageResponse<MemberListItemResponse> page = memberService.getMembers(null, pageable);

        // Assert
        MemberListItemResponse first = page.content().get(0);
        assertThat(first.name()).isEqualTo("김철수");
        assertThat(first.membership().status()).isEqualTo(MembershipPeriodStatus.ACTIVE);
        assertThat(first.membership().endDate()).isEqualTo(today.plusDays(20));
        assertThat(first.membership().daysRemaining()).isEqualTo(20L);
        assertThat(first.ptRemainingCount()).isEqualTo(12);
        // 이용권 이력이 없는 회원은 null, PT권이 없는 회원은 0회
        assertThat(page.content().get(1).membership()).isNull();
        assertThat(page.content().get(1).ptRemainingCount()).isZero();
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void 검색_결과에도_대표_이용권_요약을_담는다() {
        Pageable pageable = PageRequest.of(0, 20);
        Member found = member(3L, "박민수");
        when(memberRepository.searchActive("민수", "", pageable))
                .thenReturn(new PageImpl<>(List.of(found), pageable, 1));
        LocalDate today = BusinessTime.today();
        Membership scheduled = new Membership(found, today.plusDays(3), today.plusDays(33));
        when(representativeMembershipFinder.findByMemberIds(eq(Set.of(3L)), eq(today)))
                .thenReturn(Map.of(3L, scheduled));

        PageResponse<MemberListItemResponse> page = memberService.getMembers("민수", pageable);

        MemberListItemResponse item = page.content().get(0);
        assertThat(item.membership().status()).isEqualTo(MembershipPeriodStatus.SCHEDULED);
        // 남은 일수는 이용중일 때만 의미가 있다
        assertThat(item.membership().daysRemaining()).isNull();
    }

    private static Member member(long id, String name) {
        Member member = new Member("26090000" + id, name, null, null, null);
        // id 는 DB 가 채우는 값이라 세터가 없다. 회원별 매칭을 보려면 테스트에서만 직접 넣는다.
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
