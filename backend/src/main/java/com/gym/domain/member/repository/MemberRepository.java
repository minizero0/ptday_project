package com.gym.domain.member.repository;

import com.gym.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 출석 체크 등 회원번호 기반 조회. 없을 수 있으므로 Optional 반환 (CLAUDE.md §4.3)
    Optional<Member> findByMemberNo(String memberNo);

    // 가입 시 회원번호 중복 검사용
    boolean existsByMemberNo(String memberNo);
}
