package com.gym.domain.ptpass.service;

import com.gym.domain.ptpass.repository.PtPassRepository;
import com.gym.domain.ptpass.repository.PtPassRepository.MemberPtRemaining;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 회원별 PT 잔여 횟수 합계. 한 회원이 PT권을 여러 개 가질 수 있어 목록에서는 합계로 보여준다.
 */
@Component
public class PtRemainingCountFinder {

    private final PtPassRepository ptPassRepository;

    public PtRemainingCountFinder(PtPassRepository ptPassRepository) {
        this.ptPassRepository = ptPassRepository;
    }

    /** 여러 회원을 한 번의 조회로 가져온다. PT권이 없는 회원은 결과에 담기지 않는다. */
    public Map<Long, Integer> findByMemberIds(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return ptPassRepository.sumRemainingByMemberIds(memberIds).stream()
                .collect(Collectors.toMap(
                        MemberPtRemaining::getMemberId, row -> Math.toIntExact(row.getRemainingCount())));
    }
}
