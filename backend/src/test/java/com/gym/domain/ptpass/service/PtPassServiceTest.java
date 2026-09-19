package com.gym.domain.ptpass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.ptpass.dto.PtPassAdjustRequest;
import com.gym.domain.ptpass.dto.PtPassAdjustmentResponse;
import com.gym.domain.ptpass.dto.PtPassCreateRequest;
import com.gym.domain.ptpass.dto.PtPassResponse;
import com.gym.domain.ptpass.entity.PtPass;
import com.gym.domain.ptpass.entity.PtPassAdjustment;
import com.gym.domain.ptpass.entity.PtPassAdjustmentType;
import com.gym.domain.ptpass.exception.InsufficientPtCountException;
import com.gym.domain.ptpass.repository.PtPassAdjustmentRepository;
import com.gym.domain.ptpass.repository.PtPassRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private static Member member() {
        return new Member("260900001", "김철수", null, null, null);
    }
}
