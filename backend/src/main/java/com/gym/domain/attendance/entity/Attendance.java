package com.gym.domain.attendance.entity;

import com.gym.domain.member.entity.Member;
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
 * 출석 이력 엔티티. 회원이 입장(체크인)할 때마다 1건씩 기록된다.
 * 날짜별 현황 조회가 핵심 사용처라 checked_in_at 에 인덱스를 둔다.
 */
@Entity
@Table(
        name = "attendance",
        indexes = @Index(name = "idx_attendance_checked_in_at", columnList = "checked_in_at"))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 목록 조회 시 회원 정보(번호/이름)를 함께 내려주므로 지연 로딩으로 두고 fetch join 으로 가져온다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @CreationTimestamp
    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private Instant checkedInAt;

    protected Attendance() {
        // JPA 전용 기본 생성자
    }

    public Attendance(Member member) {
        this.member = member;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }
}
