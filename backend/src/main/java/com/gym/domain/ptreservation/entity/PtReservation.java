package com.gym.domain.ptreservation.entity;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.util.BusinessTime;
import com.gym.domain.member.entity.Member;
import com.gym.domain.ptpass.entity.PtPass;
import com.gym.domain.trainer.entity.Trainer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * PT 예약 (docs/ERD.md §3.8). 예약 1건 = PT권 1회. 길이는 직접 받지 않고 차감할 PT권의 수업 길이로 정한다.
 * 시간 겹침은 다른 예약을 봐야 알 수 있어 Service 와 DB 제약이 맡고, 여기서는 예약 1건만으로 판단되는 규칙을 지킨다.
 */
@Entity
@Table(name = "pt_reservation")
public class PtReservation {

    public static final int START_MINUTE_STEP = 10;
    public static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(23, 0);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pt_pass_id", nullable = false, updatable = false)
    private PtPass ptPass;

    // pt_pass 로도 알 수 있지만, 회원별 시간 겹침을 DB 제약으로 막으려면 이 테이블에 있어야 한다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false, updatable = false)
    private Trainer trainer;

    @Column(name = "start_time", nullable = false, updatable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false, updatable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PtReservationStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    protected PtReservation() {
        // JPA 전용 기본 생성자
    }

    private PtReservation(PtPass ptPass, Trainer trainer, Instant startTime, Instant endTime, String createdBy) {
        this.ptPass = ptPass;
        this.member = ptPass.getMember();
        this.trainer = trainer;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = PtReservationStatus.RESERVED;
        this.createdBy = createdBy;
    }

    /**
     * @param now 지난 시각 판단의 기준. 테스트에서 고정할 수 있게 밖에서 받는다.
     */
    public static PtReservation reserve(
            PtPass ptPass, Trainer trainer, Instant startTime, String createdBy, Instant now) {
        if (!trainer.isActive()) {
            throw new BusinessException(ErrorCode.TRAINER_INACTIVE);
        }
        Instant endTime = startTime.plus(Duration.ofMinutes(ptPass.getSessionMinutes()));
        validateWithinBusinessHours(startTime, endTime);
        if (startTime.isBefore(now)) {
            throw new BusinessException(ErrorCode.PT_RESERVATION_IN_PAST);
        }
        return new PtReservation(ptPass, trainer, startTime, endTime, createdBy);
    }

    private static void validateWithinBusinessHours(Instant startTime, Instant endTime) {
        ZonedDateTime start = startTime.atZone(BusinessTime.ZONE);
        ZonedDateTime end = endTime.atZone(BusinessTime.ZONE);

        boolean isOnGrid = start.getMinute() % START_MINUTE_STEP == 0
                && start.getSecond() == 0
                && start.getNano() == 0;
        boolean isSameDay = start.toLocalDate().equals(end.toLocalDate());
        boolean isWithinHours = !start.toLocalTime().isBefore(OPENING_TIME)
                && !end.toLocalTime().isAfter(CLOSING_TIME);

        if (!isOnGrid || !isSameDay || !isWithinHours) {
            throw new BusinessException(ErrorCode.INVALID_PT_RESERVATION_TIME);
        }
    }

    public void cancel(Instant now) {
        if (status == PtReservationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.PT_RESERVATION_ALREADY_CANCELED);
        }
        this.status = PtReservationStatus.CANCELED;
        this.canceledAt = now;
    }

    public Long getId() {
        return id;
    }

    public PtPass getPtPass() {
        return ptPass;
    }

    public Member getMember() {
        return member;
    }

    public Trainer getTrainer() {
        return trainer;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public PtReservationStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }
}
