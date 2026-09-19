package com.gym.domain.ptreservation.repository;

import com.gym.domain.ptreservation.entity.PtReservation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PtReservationRepository extends JpaRepository<PtReservation, Long> {

    /**
     * 취소를 위한 조회. 같은 예약의 취소가 동시에 들어오면 둘 다 "아직 예약 상태"로 읽고
     * 각자 1회를 복원해 버린다. 행을 잠가 두 번째 요청이 취소된 상태를 보게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PtReservation r where r.id = :id")
    Optional<PtReservation> findByIdForUpdate(@Param("id") Long id);

    // 겹침 판단은 반열린 구간 [start, end) 기준: 14:00~14:50 뒤에 14:50 시작은 겹치지 않는다.
    // DB 의 exclusion 제약(tstzrange 기본 경계)과 같은 기준이다.
    @Query("select count(r) > 0 from PtReservation r "
            + "where r.trainer.id = :trainerId and r.status = com.gym.domain.ptreservation.entity.PtReservationStatus.RESERVED "
            + "and r.startTime < :endTime and r.endTime > :startTime")
    boolean existsTrainerOverlap(
            @Param("trainerId") Long trainerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("select count(r) > 0 from PtReservation r "
            + "where r.member.id = :memberId and r.status = com.gym.domain.ptreservation.entity.PtReservationStatus.RESERVED "
            + "and r.startTime < :endTime and r.endTime > :startTime")
    boolean existsMemberOverlap(
            @Param("memberId") Long memberId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /** 시간표용: 트레이너 한 명의 기간 내 유효 예약. 응답에 회원·트레이너·PT권이 모두 쓰여 함께 가져온다(N+1 방지). */
    @Query("select r from PtReservation r join fetch r.member join fetch r.trainer join fetch r.ptPass "
            + "where r.trainer.id = :trainerId and r.status = com.gym.domain.ptreservation.entity.PtReservationStatus.RESERVED "
            + "and r.startTime >= :from and r.startTime < :to order by r.startTime")
    List<PtReservation> findReservedByTrainerBetween(
            @Param("trainerId") Long trainerId, @Param("from") Instant from, @Param("to") Instant to);
}
