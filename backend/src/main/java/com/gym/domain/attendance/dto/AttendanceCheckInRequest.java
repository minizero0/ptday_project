package com.gym.domain.attendance.dto;

/**
 * 출석 체크 요청. 세 가지 중 정확히 하나로 회원을 지정한다.
 * - memberNo: 회원번호 입력
 * - phoneLastDigits: 연락처 뒷 4자리 입력 (중복 시 후보 목록 응답)
 * - memberId: 뒷 4자리 중복 케이스에서 직원이 후보를 선택해 확정할 때 사용
 * 상호 배타 검증은 Service 에서 수행한다.
 */
public record AttendanceCheckInRequest(String memberNo, String phoneLastDigits, Long memberId) {}
