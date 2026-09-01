# ERD — 헬스장 관리 시스템 데이터 모델

> 확정 버전. 스키마 변경 시 이 문서를 먼저 갱신하고 마이그레이션을 진행한다.
> 표기 규칙: 테이블/컬럼은 `snake_case`(CLAUDE.md §4.2), PK는 `bigint` 자동증가, 시각은 UTC(ISO-8601).

---

## 1. 개요

8개 테이블. `member`(회원)를 중심 허브로, 각 도메인 기능이 `member`를 참조한다.
로그인 주체(관리자/직원/트레이너/회원)는 `account`로 통합하고 `role`로 구분한다.

```
account ──(0~1)── member ──< attendance
                    │  ├──< payment
                    │  ├──< membership
                    │  ├──< pt_pass ──< pt_reservation >── trainer
                    │  └──< pt_reservation
```

---

## 2. 핵심 설계 결정

1. **인증과 회원을 분리한다.** `account`(로그인 전담) ↔ `member`(고객 정보 전담).
   - 회원은 현재 로그인하지 않는 "관리 대상"이다.
   - 미래에 회원 로그인이 열리면 `account` 행을 만들고 `member.account_id`로 연결한다.
     테이블 구조 변경 없이 확장 가능하도록 **`account_id`(nullable) 씨앗**을 미리 심었다.
2. **`role`로 로그인 주체를 구분한다.** `ADMIN` / `STAFF` / `TRAINER` / `MEMBER`.
3. **회원번호(`member_no`)는 PK와 별개.** `id`는 내부 PK, `member_no`는 출석 체크용 노출 키.
   유일성 보장 + 인덱스(CLAUDE.md §9).
4. **이용권·PT권은 회원 필드가 아니라 별도 테이블.** 한 회원이 여러 번 구매 가능(1:N).
5. **결제는 이력(로그) 성격.** 결제 대상(이용권/PT권)은 `item_type` + `item_id`로 느슨하게 참조.
   한 번 기록된 결제는 수정보다 상태 전이/환불 이력으로 다룬다.
6. **금액은 정수(원 단위).** 부동소수점 금지(CLAUDE.md §9).
7. **PT 예약은 `pt_pass`를 참조.** 예약 성사 시 해당 PT권의 `remaining_count`를 차감한다.
   트레이너·시간 중복 예약은 서버에서 검증(CLAUDE.md §9, 동시성 주의).

---

## 3. 테이블 정의

### 3.1 account — 로그인 계정
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| username | varchar | unique, not null | 로그인 아이디 |
| email | varchar | nullable | 연락/알림용 |
| password_hash | varchar | not null | BCrypt 해싱(CLAUDE.md §12) |
| role | varchar | not null | ADMIN / STAFF / TRAINER / MEMBER |
| created_at | timestamp | not null | 생성 시각(UTC) |

### 3.2 member — 회원(고객)
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| account_id | bigint | FK→account.id, nullable | 미래 회원 로그인 연결(현재 null) |
| member_no | varchar | unique, not null, index | 출석 체크용 회원번호 |
| name | varchar | not null | 이름 |
| phone | varchar | nullable | 연락처 |
| gender | varchar | nullable | 성별 |
| birth_date | date | nullable | 생년월일 |
| created_at | timestamp | not null | 등록 시각(UTC) |

### 3.3 attendance — 출석
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| member_id | bigint | FK→member.id, not null | 출석 회원 |
| checked_in_at | timestamp | not null | 입장 시각(UTC) |

### 3.4 payment — 결제 이력
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| member_id | bigint | FK→member.id, not null | 결제 회원 |
| amount | int | not null | 금액(원 단위) |
| method | varchar | not null | 결제 수단(CARD/CASH 등) |
| item_type | varchar | nullable | 결제 대상 종류(MEMBERSHIP/PT_PASS) |
| item_id | bigint | nullable | 결제 대상 참조 id(느슨한 연결) |
| status | varchar | not null | PAID / CANCELED / REFUNDED |
| paid_at | timestamp | not null | 결제 시각(UTC) |

### 3.5 membership — 헬스 이용권
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| member_id | bigint | FK→member.id, not null | 보유 회원 |
| start_date | date | not null | 시작일 |
| months | int | not null | 이용 개월 수 |
| end_date | date | not null | 만료일(시작일 + 개월 수) |
| status | varchar | not null | ACTIVE / EXPIRED |
| created_at | timestamp | not null | 생성 시각(UTC) |

> 만료 여부 판단 로직은 Service에 둔다(CLAUDE.md §9).

### 3.6 pt_pass — PT권
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| member_id | bigint | FK→member.id, not null | 보유 회원 |
| total_count | int | not null | 총 PT 횟수 |
| remaining_count | int | not null | 잔여 횟수(음수 불가) |
| created_at | timestamp | not null | 생성 시각(UTC) |

> 차감 시 음수 방지 검증(`InsufficientPtCountException`), `@Transactional`로 원자성 보장(CLAUDE.md §7, §9).

### 3.7 trainer — 트레이너
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| name | varchar | not null | 이름 |
| phone | varchar | nullable | 연락처 |

> 트레이너 로그인이 필요해지면 `member`와 동일하게 `account_id`(nullable)를 추가한다(현재 미도입).

### 3.8 pt_reservation — PT 예약
| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| id | bigint | PK | 자동증가 |
| pt_pass_id | bigint | FK→pt_pass.id, not null | 차감 대상 PT권 |
| member_id | bigint | FK→member.id, not null | 예약 회원 |
| trainer_id | bigint | FK→trainer.id, not null | 담당 트레이너 |
| start_time | timestamp | not null | 예약 시작(UTC) |
| end_time | timestamp | not null | 예약 종료(UTC) |
| status | varchar | not null | RESERVED / DONE / CANCELED |

> 동일 트레이너의 시간 중복 예약을 서버에서 검증한다(동시성 주의).

---

## 4. 관계 요약

| 관계 | 종류 | 의미 |
| --- | --- | --- |
| account — member | 1 : 0~1 | 계정이 회원과 연결(선택, 미래 확장) |
| member — attendance | 1 : N | 회원의 출석 이력 |
| member — payment | 1 : N | 회원의 결제 이력 |
| member — membership | 1 : N | 회원의 이용권 |
| member — pt_pass | 1 : N | 회원의 PT권 |
| member — pt_reservation | 1 : N | 회원의 예약 |
| trainer — pt_reservation | 1 : N | 트레이너 담당 예약 |
| pt_pass — pt_reservation | 1 : N | 예약 시 해당 PT권 차감 |

---

## 5. 아직 도입하지 않은 것 (향후 확장 후보)

- `refund` 테이블(환불 이력) — 현재는 `payment.status`로만 표현.
- 공통 컬럼(`updated_at`, soft-delete 플래그) — 확정 후 일괄 적용 검토.
- 트레이너 근무 스케줄 테이블 — 우선 예약 자체부터. 스케줄 관리는 이후 확장.
- `trainer.account_id` — 트레이너 로그인 시나리오 구체화 시 추가.
