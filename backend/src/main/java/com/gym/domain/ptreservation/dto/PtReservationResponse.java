package com.gym.domain.ptreservation.dto;

import com.gym.domain.member.entity.Member;
import com.gym.domain.ptreservation.entity.PtReservation;
import com.gym.domain.ptreservation.entity.PtReservationStatus;
import com.gym.domain.trainer.entity.Trainer;
import java.time.Instant;

/** 시간표 한 칸에 필요한 값을 모두 담는다 — 화면이 회원·트레이너를 다시 조회하지 않게 한다. */
public record PtReservationResponse(
        Long id,
        Long memberId,
        String memberNo,
        String memberName,
        Long trainerId,
        String trainerName,
        Long ptPassId,
        int sessionMinutes,
        Instant startTime,
        Instant endTime,
        PtReservationStatus status,
        String createdBy,
        Instant createdAt,
        Instant canceledAt) {

    public static PtReservationResponse from(PtReservation reservation) {
        Member member = reservation.getMember();
        Trainer trainer = reservation.getTrainer();
        return new PtReservationResponse(
                reservation.getId(),
                member.getId(),
                member.getMemberNo(),
                member.getName(),
                trainer.getId(),
                trainer.getName(),
                reservation.getPtPass().getId(),
                reservation.getPtPass().getSessionMinutes(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getCreatedBy(),
                reservation.getCreatedAt(),
                reservation.getCanceledAt());
    }
}
