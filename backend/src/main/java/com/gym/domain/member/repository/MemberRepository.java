package com.gym.domain.member.repository;

import com.gym.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 출석 체크 등 회원번호 기반 조회. 없을 수 있으므로 Optional 반환 (CLAUDE.md §4.3)
    Optional<Member> findByMemberNo(String memberNo);

    // 가입 시 회원번호 중복 검사용
    boolean existsByMemberNo(String memberNo);

    // 자동 채번용: 해당 연도 접두사로 시작하는 회원번호 중 가장 큰 값(= 그 해 마지막 발급 번호).
    // 삭제된 회원도 포함해 전체를 조회한다(번호 재사용에 따른 UNIQUE 충돌 방지).
    Optional<Member> findTopByMemberNoStartingWithOrderByMemberNoDesc(String yearPrefix);

    // 조회용: soft delete 되지 않은 활성 회원만 대상으로 한다.
    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    Page<Member> findAllByDeletedAtIsNull(Pageable pageable);
}
