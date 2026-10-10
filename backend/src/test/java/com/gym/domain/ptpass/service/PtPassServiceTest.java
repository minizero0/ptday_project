package com.gym.domain.ptpass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.response.PageResponse;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.ptpass.dto.PtPassAdjustRequest;
import com.gym.domain.ptpass.dto.PtPassAdjustmentResponse;
import com.gym.domain.ptpass.dto.PtPassCreateRequest;
import com.gym.domain.ptpass.dto.PtPassListItemResponse;
import com.gym.domain.ptpass.dto.PtPassResponse;
import com.gym.domain.ptpass.entity.PtPass;
import com.gym.domain.ptpass.entity.PtPassAdjustment;
import com.gym.domain.ptpass.entity.PtPassAdjustmentType;
import com.gym.domain.ptpass.exception.InsufficientPtCountException;
import com.gym.domain.ptpass.repository.PtPassAdjustmentRepository;
import com.gym.domain.ptpass.repository.PtPassRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PtPassServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long PT_PASS_ID = 7L;
    private static final String STAFF = "desk01";

    @Mock
    private PtPassRepository ptPassRepository;

    @Mock
    private PtPassAdjustmentRepository ptPassAdjustmentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PtPassService ptPassService;

    @Test
    void PT권을_부여하면_잔여_횟수가_구매_횟수와_같게_저장된다() {
        Member member = member();
        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.of(member));
        when(ptPassRepository.save(any(PtPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PtPassResponse response = ptPassService.grant(MEMBER_ID, new PtPassCreateRequest(50, 10));

        assertThat(response.sessionMinutes()).isEqualTo(50);
        assertThat(response.totalCount()).isEqualTo(10);
        assertThat(response.remainingCount()).isEqualTo(10);
    }

    @Test
    void 없는_회원에게는_PT권을_부여할_수_없다() {
        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ptPassService.grant(MEMBER_ID, new PtPassCreateRequest(50, 10)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(ptPassRepository, never()).save(any());
    }

    @Test
    void 횟수를_조정하면_잔여_횟수가_바뀌고_누가_왜_바꿨는지_이력이_남는다() {
        PtPass ptPass = new PtPass(member(), 50, 10);
        when(ptPassRepository.findByIdForUpdate(PT_PASS_ID)).thenReturn(Optional.of(ptPass));

        PtPassResponse response =
                ptPassService.adjust(PT_PASS_ID, new PtPassAdjustRequest(2, "  서비스 2회 추가  "), STAFF);

        assertThat(response.remainingCount()).isEqualTo(12);
        assertThat(response.totalCount()).isEqualTo(10);

        ArgumentCaptor<PtPassAdjustment> saved = ArgumentCaptor.forClass(PtPassAdjustment.class);
        verify(ptPassAdjustmentRepository).save(saved.capture());
        PtPassAdjustment adjustment = saved.getValue();
        assertThat(adjustment.getType()).isEqualTo(PtPassAdjustmentType.MANUAL);
        assertThat(adjustment.getDelta()).isEqualTo(2);
        assertThat(adjustment.getRemainingAfter()).isEqualTo(12);
        assertThat(adjustment.getReason()).isEqualTo("서비스 2회 추가");
        assertThat(adjustment.getAdjustedBy()).isEqualTo(STAFF);
    }

    @Test
    void 잔여_횟수가_모자란_차감_조정은_거절되고_이력도_남지_않는다() {
        PtPass ptPass = new PtPass(member(), 50, 2);
        when(ptPassRepository.findByIdForUpdate(PT_PASS_ID)).thenReturn(Optional.of(ptPass));

        assertThatThrownBy(() ->
                ptPassService.adjust(PT_PASS_ID, new PtPassAdjustRequest(-3, "정정"), STAFF))
                .isInstanceOf(InsufficientPtCountException.class);

        assertThat(ptPass.getRemainingCount()).isEqualTo(2);
        verify(ptPassAdjustmentRepository, never()).save(any());
    }

    @Test
    void 없는_PT권은_조정할_수_없다() {
        when(ptPassRepository.findByIdForUpdate(PT_PASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                ptPassService.adjust(PT_PASS_ID, new PtPassAdjustRequest(1, "추가"), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PT_PASS_NOT_FOUND);
    }

    @Test
    void 삭제된_회원의_PT권은_조정할_수_없다() {
        Member deleted = member();
        deleted.delete();
        when(ptPassRepository.findByIdForUpdate(PT_PASS_ID)).thenReturn(Optional.of(new PtPass(deleted, 50, 10)));

        assertThatThrownBy(() ->
                ptPassService.adjust(PT_PASS_ID, new PtPassAdjustRequest(1, "추가"), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PT_PASS_NOT_FOUND);
        verify(ptPassAdjustmentRepository, never()).save(any());
    }

    @Test
    void 회원의_PT권_목록을_조회한다() {
        Member member = member();
        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.of(member));
        PtPass used = new PtPass(member, 50, 10);
        used.deduct(4);
        when(ptPassRepository.findByMemberIdOrderByCreatedAtDescIdDesc(MEMBER_ID)).thenReturn(List.of(used));

        List<PtPassResponse> responses = ptPassService.getPtPassesByMember(MEMBER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).remainingCount()).isEqualTo(6);
    }

    @Test
    void 조정_이력을_조회한다() {
        PtPass ptPass = new PtPass(member(), 50, 10);
        ptPass.adjust(2);
        when(ptPassRepository.findById(PT_PASS_ID)).thenReturn(Optional.of(ptPass));
        when(ptPassAdjustmentRepository.findByPtPassIdOrderByCreatedAtDescIdDesc(PT_PASS_ID))
                .thenReturn(List.of(PtPassAdjustment.manual(ptPass, 2, "서비스", STAFF)));

        List<PtPassAdjustmentResponse> responses = ptPassService.getAdjustments(PT_PASS_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).delta()).isEqualTo(2);
        assertThat(responses.get(0).remainingAfter()).isEqualTo(12);
        assertThat(responses.get(0).adjustedBy()).isEqualTo(STAFF);
    }

    @Test
    void 관리_목록은_조건이_없으면_전체_기간을_빈_검색어로_조회해_회원_정보와_함께_내려준다() {
        Pageable pageable = PageRequest.of(0, 20);
        PtPass ptPass = new PtPass(member(), 50, 10);
        ptPass.deduct(3);
        when(ptPassRepository.search(
                        false, "", Instant.EPOCH, Instant.parse("9999-12-31T00:00:00Z"), pageable))
                .thenReturn(new PageImpl<>(List.of(ptPass), pageable, 1));

        PageResponse<PtPassListItemResponse> response =
                ptPassService.getPtPasses(false, null, null, null, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        PtPassListItemResponse item = response.content().get(0);
        assertThat(item.memberNo()).isEqualTo("260900001");
        assertThat(item.memberName()).isEqualTo("김철수");
        assertThat(item.sessionMinutes()).isEqualTo(50);
        assertThat(item.totalCount()).isEqualTo(10);
        assertThat(item.remainingCount()).isEqualTo(7);
    }

    @Test
    void 관리_목록의_구매일_기간은_한국_시간의_하루로_잘라_종료일_끝까지_포함한다() {
        Pageable pageable = PageRequest.of(0, 20);
        // 10/01 00:00 KST = 09/30 15:00 UTC, 10/10 이 끝나는 10/11 00:00 KST = 10/10 15:00 UTC
        when(ptPassRepository.search(
                        true,
                        "철수",
                        Instant.parse("2026-09-30T15:00:00Z"),
                        Instant.parse("2026-10-10T15:00:00Z"),
                        pageable))
                .thenReturn(new PageImpl<>(List.of(new PtPass(member(), 30, 5)), pageable, 1));

        PageResponse<PtPassListItemResponse> response = ptPassService.getPtPasses(
                true, "  철수  ", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), pageable);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void 관리_목록의_구매일_종료일이_시작일보다_빠르면_거절한다() {
        assertThatThrownBy(() -> ptPassService.getPtPasses(
                        false, null, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 9), PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PT_PASS_SEARCH_PERIOD);
        verify(ptPassRepository, never()).search(anyBoolean(), any(), any(), any(), any());
    }

    private static Member member() {
        return new Member("260900001", "김철수", null, null, null);
    }
}
