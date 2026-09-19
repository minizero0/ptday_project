package com.gym.domain.ptpass.repository;

import com.gym.domain.ptpass.entity.PtPass;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PtPassRepository extends JpaRepository<PtPass, Long> {

    // 회원 상세의 PT권 목록: 최근 구매분이 위로
    List<PtPass> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    /**
     * 잔여 횟수를 바꾸기 위한 조회. 같은 PT권에 조정·차감이 동시에 들어와도 한 번에 하나씩만 반영되도록
     * 행을 잠근다. 잠그지 않으면 두 요청이 같은 잔여 횟수를 읽고 각자 계산해 한쪽 변경이 사라진다 (CLAUDE.md §9).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PtPass p where p.id = :id")
    Optional<PtPass> findByIdForUpdate(@Param("id") Long id);

    /** 회원 목록용: 여러 회원의 잔여 횟수 합계를 한 번에 가져온다(회원마다 조회하면 N+1). */
    @Query("select p.member.id as memberId, sum(p.remainingCount) as remainingCount "
            + "from PtPass p where p.member.id in :memberIds group by p.member.id")
    List<MemberPtRemaining> sumRemainingByMemberIds(@Param("memberIds") Collection<Long> memberIds);

    interface MemberPtRemaining {
        Long getMemberId();

        Long getRemainingCount();
    }
}
