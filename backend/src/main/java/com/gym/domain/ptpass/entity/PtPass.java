package com.gym.domain.ptpass.entity;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.member.entity.Member;
import com.gym.domain.ptpass.exception.InsufficientPtCountException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * PT권 (docs/ERD.md §3.6). 한 회원이 여러 번 구매할 수 있는 1:N 이력이며 유효 기간은 없다.
 * 구매 횟수(totalCount)는 부여 후 바뀌지 않고, 잔여 횟수(remainingCount)만 예약 차감·수동 조정으로 움직인다.
 * 잔여 횟수가 음수가 되지 않는다는 규칙을 이 엔티티가 직접 지킨다 (CLAUDE.md §9).
 */
@Entity
@Table(name = "pt_pass", indexes = @Index(name = "idx_pt_pass_member_id", columnList = "member_id"))
public class PtPass {

    // 한 번에 부여하거나 보유할 수 있는 횟수 상한. 오타(10 → 1000)로 인한 사고를 막는 안전장치다.
    public static final int MAX_PT_COUNT = 999;
    private static final int MIN_PT_COUNT = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "total_count", nullable = false, updatable = false)
    private int totalCount;

    @Column(name = "remaining_count", nullable = false)
    private int remainingCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PtPass() {
        // JPA 전용 기본 생성자
    }

    public PtPass(Member member, int totalCount) {
        if (totalCount < MIN_PT_COUNT || totalCount > MAX_PT_COUNT) {
            throw new IllegalArgumentException(
                    "PT 횟수는 " + MIN_PT_COUNT + "~" + MAX_PT_COUNT + " 사이여야 합니다. totalCount=" + totalCount);
        }
        this.member = member;
        this.totalCount = totalCount;
        this.remainingCount = totalCount;
    }

    /** PT 이용(예약)에 따른 차감. */
    public void deduct(int count) {
        if (count < MIN_PT_COUNT) {
            throw new IllegalArgumentException("차감 횟수는 1 이상이어야 합니다. count=" + count);
        }
        if (count > remainingCount) {
            throw new InsufficientPtCountException();
        }
        this.remainingCount -= count;
    }

    /**
     * 직원의 수동 조정. 서비스 추가(+)와 정정 차감(-) 모두 잔여 횟수만 바꾼다.
     * 구매 횟수까지 바꾸면 "잘못 차감된 1회 복구" 같은 정정에서도 구매 횟수가 늘어나 버린다.
     */
    public void adjust(int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("증감 횟수는 0일 수 없습니다.");
        }
        int adjusted = remainingCount + delta;
        if (adjusted < 0) {
            throw new InsufficientPtCountException();
        }
        if (adjusted > MAX_PT_COUNT) {
            throw new BusinessException(ErrorCode.PT_COUNT_LIMIT_EXCEEDED);
        }
        this.remainingCount = adjusted;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getRemainingCount() {
        return remainingCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
