package com.gym.domain.member.repository;

import com.gym.domain.member.entity.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 출석 체크용: 회원번호로 활성 회원 조회
    Optional<Member> findByMemberNoAndDeletedAtIsNull(String memberNo);

    // 출석 체크용: 연락처 뒷자리가 일치하는 활성 회원 목록.
    // 하이픈 등 표기 차이를 무시하기 위해 숫자만 남겨 비교한다. 중복 시 호출부에서 후보 선택 처리.
    @Query("""
            select m from Member m
            where m.deletedAt is null
              and replace(coalesce(m.phone, ''), '-', '') like concat('%', :digits)
            """)
    List<Member> findActiveByPhoneEndingWith(@Param("digits") String digits);

    /**
     * 회원 검색: 이름·회원번호·전화번호를 한 번에 훑는다(이용권 등록 시 회원 선택용).
     * digits 는 검색어에서 숫자만 남긴 값. 숫자가 없으면 빈 문자열로 넘기고 :digits <> '' 로 조건을 끈다.
     * null 을 넘기면 PostgreSQL 이 파라미터 타입을 추론하지 못해 like 비교에서 실패한다
     * (operator does not exist: text ~~ bytea).
     */
    @Query("""
            select m from Member m
            where m.deletedAt is null
              and (lower(m.name) like lower(concat('%', :keyword, '%'))
                   or m.memberNo like concat('%', :keyword, '%')
                   or (:digits <> ''
                       and replace(coalesce(m.phone, ''), '-', '') like concat('%', :digits, '%')))
            """)
    Page<Member> searchActive(
            @Param("keyword") String keyword, @Param("digits") String digits, Pageable pageable);
}
