package com.gym.domain.membership.repository;

import com.gym.domain.membership.entity.Membership;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // 회원 상세 패널의 "이용권 이력": 최근 구매분이 위로 오도록 시작일 내림차순.
    // 같은 날 여러 건이면 나중에 등록된 것(id 큰 값)이 먼저다.
    List<Membership> findByMemberIdOrderByStartDateDescIdDesc(Long memberId);

    // 출석 그리드용: 여러 회원의 이용권을 한 번에 가져온다(회원마다 조회하면 N+1).
    // 회원별로 어떤 건을 대표로 보여줄지는 날짜 판단이 필요해 Service 에서 고른다.
    List<Membership> findByMemberIdInOrderByStartDateDescIdDesc(Collection<Long> memberIds);

    /**
     * 이용권 관리 목록(기본): 아직 끝나지 않은 이용권을 만료가 임박한 순으로.
     * 만료건까지 섞으면 몇 년 전 건이 맨 위로 올라와 목록이 쓸모없어지므로 여기서 제외한다.
     * member 는 목록에 회원번호·이름을 함께 내리므로 fetch join 한다(ManyToOne 이라 페이징 안전).
     */
    @Query(
            value = "select m from Membership m join fetch m.member mem "
                    + "where mem.deletedAt is null "
                    + "  and m.status = com.gym.domain.membership.entity.MembershipStatus.ACTIVE "
                    + "  and m.endDate >= :today "
                    + "order by m.endDate asc, m.id asc",
            countQuery = "select count(m) from Membership m "
                    + "where m.member.deletedAt is null "
                    + "  and m.status = com.gym.domain.membership.entity.MembershipStatus.ACTIVE "
                    + "  and m.endDate >= :today")
    Page<Membership> findOngoing(@Param("today") LocalDate today, Pageable pageable);

    /** 만료건까지 포함한 전체 목록. 최근에 끝난 것부터 본다. */
    @Query(
            value = "select m from Membership m join fetch m.member mem "
                    + "where mem.deletedAt is null "
                    + "order by m.endDate desc, m.id desc",
            countQuery = "select count(m) from Membership m where m.member.deletedAt is null")
    Page<Membership> findAllIncludingExpired(Pageable pageable);
}
