package com.gym.domain.member.service;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.response.PageResponse;
import com.gym.domain.member.dto.MemberCreateRequest;
import com.gym.domain.member.dto.MemberResponse;
import com.gym.domain.member.dto.MemberUpdateRequest;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    // 회원번호 형식: YY(연2) + MM(월2) + 일련번호(5). 매월 초기화. 예) 260900001
    private static final DateTimeFormatter PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyMM");
    private static final int PREFIX_LENGTH = 4;
    private static final String SEQUENCE_FORMAT = "%05d";
    private static final int FIRST_SEQUENCE = 1;
    private static final int MAX_SEQUENCE = 99_999;

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberResponse createMember(MemberCreateRequest request) {
        String memberNo = generateMemberNo();
        Member member = new Member(
                memberNo,
                request.name(),
                request.phone(),
                request.gender(),
                request.birthDate());
        Member saved = memberRepository.save(member);
        return MemberResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> getMembers(Pageable pageable) {
        return PageResponse.from(memberRepository.findAll(pageable).map(MemberResponse::from));
    }

    @Transactional
    public MemberResponse updateMember(Long id, MemberUpdateRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        // 변경 감지(dirty checking): 트랜잭션 커밋 시점에 자동 UPDATE 되므로 save() 불필요
        member.updateInfo(request.name(), request.phone(), request.gender(), request.birthDate());
        return MemberResponse.from(member);
    }

    /**
     * 이번 달 마지막 발급 번호 다음 값으로 회원번호를 만든다.
     * 접두사(YYMM) 안에서 자릿수가 고정이라 문자열 정렬로 최댓값을 찾을 수 있다.
     * member_no 의 UNIQUE 제약이 최종 방어선이며, 고동시성 환경에서는 별도 채번 전략(시퀀스 등)으로 강화한다.
     */
    private String generateMemberNo() {
        String prefix = LocalDate.now().format(PREFIX_FORMATTER);
        int nextSequence = memberRepository
                .findTopByMemberNoStartingWithOrderByMemberNoDesc(prefix)
                .map(latest -> parseSequence(latest.getMemberNo()) + 1)
                .orElse(FIRST_SEQUENCE);

        if (nextSequence > MAX_SEQUENCE) {
            throw new IllegalStateException(
                    "이번 달 회원번호가 모두 소진되었습니다. 채번 자릿수 확장이 필요합니다. prefix=" + prefix);
        }
        return prefix + String.format(SEQUENCE_FORMAT, nextSequence);
    }

    private int parseSequence(String memberNo) {
        return Integer.parseInt(memberNo.substring(PREFIX_LENGTH));
    }
}
