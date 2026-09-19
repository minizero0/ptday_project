package com.gym.domain.ptpass.service;

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
import com.gym.domain.ptpass.repository.PtPassAdjustmentRepository;
import com.gym.domain.ptpass.repository.PtPassRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PtPassService {

    private static final Logger log = LoggerFactory.getLogger(PtPassService.class);

    private final PtPassRepository ptPassRepository;
    private final PtPassAdjustmentRepository ptPassAdjustmentRepository;
    private final MemberRepository memberRepository;

    public PtPassService(
            PtPassRepository ptPassRepository,
            PtPassAdjustmentRepository ptPassAdjustmentRepository,
            MemberRepository memberRepository) {
        this.ptPassRepository = ptPassRepository;
        this.ptPassAdjustmentRepository = ptPassAdjustmentRepository;
        this.memberRepository = memberRepository;
    }

    /** 회원에게 PT권을 부여한다. 기존 PT권이 남아 있어도 막지 않는다 — 추가 구매는 이력으로 쌓인다. */
    @Transactional
    public PtPassResponse grant(Long memberId, PtPassCreateRequest request) {
        Member member = getActiveMember(memberId);
        PtPass saved = ptPassRepository.save(new PtPass(member, request.sessionMinutes(), request.totalCount()));
        return PtPassResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PtPassResponse> getPtPassesByMember(Long memberId) {
        getActiveMember(memberId);
        return ptPassRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                .map(PtPassResponse::from)
                .toList();
    }

    /**
     * 직원의 수동 조정. 잔여 횟수 변경과 이력 기록은 한 트랜잭션이다 —
     * 횟수만 바뀌고 근거가 남지 않는 일이 없어야 한다 (CLAUDE.md §7.1).
     *
     * @param adjustedBy 처리한 계정 아이디. 요청 본문이 아니라 로그인 정보에서 온 값이어야 한다.
     */
    @Transactional
    public PtPassResponse adjust(Long ptPassId, PtPassAdjustRequest request, String adjustedBy) {
        PtPass ptPass = ptPassRepository.findByIdForUpdate(ptPassId)
                .filter(found -> !found.getMember().isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.PT_PASS_NOT_FOUND));

        ptPass.adjust(request.delta());
        ptPassAdjustmentRepository.save(
                PtPassAdjustment.manual(ptPass, request.delta(), request.reason().trim(), adjustedBy));

        log.info("PT 횟수 수동 조정: ptPassId={}, delta={}, remainingAfter={}, adjustedBy={}",
                ptPassId, request.delta(), ptPass.getRemainingCount(), adjustedBy);
        return PtPassResponse.from(ptPass);
    }

    @Transactional(readOnly = true)
    public List<PtPassAdjustmentResponse> getAdjustments(Long ptPassId) {
        if (ptPassRepository.findById(ptPassId).isEmpty()) {
            throw new BusinessException(ErrorCode.PT_PASS_NOT_FOUND);
        }
        return ptPassAdjustmentRepository.findByPtPassIdOrderByCreatedAtDescIdDesc(ptPassId).stream()
                .map(PtPassAdjustmentResponse::from)
                .toList();
    }

    private Member getActiveMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
