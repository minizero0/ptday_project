package com.gym.domain.membership.service;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.response.PageResponse;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.membership.dto.MembershipCreateRequest;
import com.gym.domain.membership.dto.MembershipListItemResponse;
import com.gym.domain.membership.dto.MembershipResponse;
import com.gym.domain.membership.dto.MembershipUpdateRequest;
import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.repository.MembershipRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

    // 이용권의 "오늘"은 헬스장 영업 기준(한국) 날짜다. 저장/전송은 UTC 기준 (CLAUDE.md §5)
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final MembershipRepository membershipRepository;
    private final MemberRepository memberRepository;

    public MembershipService(
            MembershipRepository membershipRepository, MemberRepository memberRepository) {
        this.membershipRepository = membershipRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 회원에게 이용권을 부여한다. 기간은 시작일·만료일로만 정한다.
     * 기존 이용권이 남아 있어도 막지 않는다 — 갱신/중복 구매는 이력으로 쌓인다 (docs/ERD.md §2-4).
     */
    @Transactional
    public MembershipResponse grant(Long memberId, MembershipCreateRequest request) {
        Member member = memberRepository
                .findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validateEndDate(request.startDate(), request.endDate());

        Membership saved = membershipRepository.save(
                new Membership(member, request.startDate(), request.endDate()));

        return MembershipResponse.from(saved, today());
    }

    /**
     * 이용권 기간 정정. 시작일·만료일을 함께 바꾼다.
     * 변경 감지(dirty checking)로 커밋 시점에 UPDATE 되므로 save() 를 부르지 않는다.
     */
    @Transactional
    public MembershipResponse updatePeriod(Long membershipId, MembershipUpdateRequest request) {
        Membership membership = membershipRepository
                .findById(membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        validateEndDate(request.startDate(), request.endDate());
        membership.updatePeriod(request.startDate(), request.endDate());

        return MembershipResponse.from(membership, today());
    }

    /**
     * 이용권 관리 목록. 기본은 아직 끝나지 않은 이용권을 만료 임박순으로 보여준다.
     * includeExpired 를 켜면 만료건까지 포함해 최근에 끝난 순으로 본다.
     */
    @Transactional(readOnly = true)
    public PageResponse<MembershipListItemResponse> getMemberships(
            boolean includeExpired, Pageable pageable) {
        LocalDate today = today();
        Page<Membership> page = includeExpired
                ? membershipRepository.findAllIncludingExpired(pageable)
                : membershipRepository.findOngoing(today, pageable);

        return PageResponse.from(
                page.map(membership -> MembershipListItemResponse.from(membership, today)));
    }

    /** 회원 상세 패널용 이용권 이력. 각 건의 "이용중" 여부는 오늘 기준으로 판단해 함께 내려준다. */
    @Transactional(readOnly = true)
    public List<MembershipResponse> getMembershipsByMember(Long memberId) {
        if (memberRepository.findByIdAndDeletedAtIsNull(memberId).isEmpty()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        LocalDate today = today();
        return membershipRepository.findByMemberIdOrderByStartDateDescIdDesc(memberId).stream()
                .map(membership -> MembershipResponse.from(membership, today))
                .toList();
    }

    /**
     * 만료일 검증. 만료일은 화면에서 확정해 보내는 값이며 서버가 대신 계산하지 않는다.
     * 만료일 당일까지 이용 가능하므로 시작일과 같은 날짜는 허용한다 (docs/ERD.md §3.5).
     */
    private void validateEndDate(LocalDate startDate, LocalDate endDate) {
        if (endDate == null) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_END_DATE_REQUIRED);
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_MEMBERSHIP_PERIOD);
        }
    }

    private LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
