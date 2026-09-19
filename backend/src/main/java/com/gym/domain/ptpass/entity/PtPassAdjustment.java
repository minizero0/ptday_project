package com.gym.domain.ptpass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * PT 횟수 변동 이력 (docs/ERD.md §3.6.1). 추가만 하고 고치거나 지우지 않는다 —
 * 회원과 횟수로 다툼이 생겼을 때 누가 언제 왜 바꿨는지 보여주는 근거다. 그래서 세터가 없다.
 */
@Entity
@Table(
        name = "pt_pass_adjustment",
        indexes = @Index(name = "idx_pt_pass_adjustment_pt_pass_id", columnList = "pt_pass_id"))
public class PtPassAdjustment {

    // 예약 1건은 PT 1회를 쓴다
    public static final int RESERVATION_COUNT = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pt_pass_id", nullable = false, updatable = false)
    private PtPass ptPass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PtPassAdjustmentType type;

    @Column(nullable = false, updatable = false)
    private int delta;

    // 반영 직후의 잔여 횟수. 이력만 보고도 "10 → 12 → 11" 흐름을 읽을 수 있게 한다.
    @Column(name = "remaining_after", nullable = false, updatable = false)
    private int remainingAfter;

    @Column(nullable = false, updatable = false)
    private String reason;

    // 처리한 계정 아이디. 요청 본문이 아니라 로그인 정보에서 채운다.
    @Column(name = "adjusted_by", nullable = false, updatable = false)
    private String adjustedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PtPassAdjustment() {
        // JPA 전용 기본 생성자
    }

    private PtPassAdjustment(
            PtPass ptPass, PtPassAdjustmentType type, int delta, String reason, String adjustedBy) {
        this.ptPass = ptPass;
        this.type = type;
        this.delta = delta;
        this.remainingAfter = ptPass.getRemainingCount();
        this.reason = reason;
        this.adjustedBy = adjustedBy;
    }

    /** 이미 조정이 반영된 PT권을 받아 그 결과를 기록한다. */
    public static PtPassAdjustment manual(PtPass adjustedPtPass, int delta, String reason, String adjustedBy) {
        return new PtPassAdjustment(adjustedPtPass, PtPassAdjustmentType.MANUAL, delta, reason, adjustedBy);
    }

    /** 예약으로 이미 1회가 차감된 PT권을 받아 그 결과를 기록한다. */
    public static PtPassAdjustment reservation(PtPass deductedPtPass, String reason, String adjustedBy) {
        return new PtPassAdjustment(
                deductedPtPass, PtPassAdjustmentType.RESERVATION, -RESERVATION_COUNT, reason, adjustedBy);
    }

    /** 예약 취소로 이미 1회가 복원된 PT권을 받아 그 결과를 기록한다. */
    public static PtPassAdjustment reservationCancel(PtPass restoredPtPass, String reason, String adjustedBy) {
        return new PtPassAdjustment(
                restoredPtPass, PtPassAdjustmentType.RESERVATION_CANCEL, RESERVATION_COUNT, reason, adjustedBy);
    }

    public Long getId() {
        return id;
    }

    public PtPass getPtPass() {
        return ptPass;
    }

    public PtPassAdjustmentType getType() {
        return type;
    }

    public int getDelta() {
        return delta;
    }

    public int getRemainingAfter() {
        return remainingAfter;
    }

    public String getReason() {
        return reason;
    }

    public String getAdjustedBy() {
        return adjustedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
