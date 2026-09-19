package com.gym.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gym.domain.member.dto.MemberCreateRequest;
import com.gym.domain.member.dto.MemberResponse;
import com.gym.domain.member.dto.MemberUpdateRequest;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);
    private static final long MEMBER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

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
}
