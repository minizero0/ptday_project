package com.gym.domain.membership.entity;

import com.gym.domain.member.entity.Member;
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
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 헬스 이용권 (docs/ERD.md §3.5). 한 회원이 여러 번 구매할 수 있는 1:N 이력이다.
 * 기간의 진실은 시작일·만료일 두 값뿐이다. 개월 수는 화면에서 만료일을 채우는 계산 수단일 뿐
 * 저장하지 않는다. 만료 판단은 MembershipPeriod 에 둔다 (CLAUDE.md §9).
 */
@Entity
@Table(
        name = "membership",
        indexes = @Index(name = "idx_membership_member_id", columnList = "member_id"))
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Membership() {
        // JPA 전용 기본 생성자
    }

    public Membership(Member member, LocalDate startDate, LocalDate endDate) {
        this.member = member;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = MembershipStatus.ACTIVE;
    }

    // 관리자의 기간 정정(시작일 변경/연장/단축). 두 값이 함께 움직여야 앞뒤가 맞아 한 번에 바꾼다.
    public void updatePeriod(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void expire() {
        this.status = MembershipStatus.EXPIRED;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
