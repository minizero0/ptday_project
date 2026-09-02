package com.gym.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 회원(고객) 엔티티. 로그인 주체가 아니라 관리 대상이다.
 * 인증 계정(account)과는 분리하며, 미래 회원 로그인 대비용으로 accountId 씨앗만 둔다.
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 미래 회원 로그인 연결용 씨앗. Account 엔티티 도입 시 @ManyToOne 관계로 승격한다.
    @Column(name = "account_id")
    private Long accountId;

    // 출석 체크의 핵심 키. 유일성 보장 + 조회 성능을 위해 인덱스가 걸린다.
    @Column(name = "member_no", nullable = false, unique = true)
    private String memberNo;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // soft delete 표시. null 이면 활성 회원, 값이 있으면 삭제된 것으로 간주한다.
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Member() {
        // JPA 전용 기본 생성자
    }

    public Member(String memberNo, String name, String phone, String gender, LocalDate birthDate) {
        this.memberNo = memberNo;
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    // 수정 가능한 정보만 바꾸는 의도 명시 메서드. member_no 등 불변 값은 건드리지 않는다.
    public void updateInfo(String name, String phone, String gender, LocalDate birthDate) {
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    // soft delete: 실제로 지우지 않고 삭제 시각만 기록한다.
    public void delete() {
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getMemberNo() {
        return memberNo;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
