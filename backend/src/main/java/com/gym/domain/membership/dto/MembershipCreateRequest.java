package com.gym.domain.membership.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 이용권 부여 요청. 기간은 시작일·만료일 두 값으로만 정한다.
 * 화면의 1/3/6/12 버튼은 만료일을 채워 주는 계산 수단일 뿐 서버로 보내지 않는다.
 * 대상 회원은 경로 변수(memberId)로 받으므로 본문에 담지 않는다.
 */
public record MembershipCreateRequest(
        @NotNull(message = "시작일을 입력하세요.")
        LocalDate startDate,

        @NotNull(message = "만료일을 입력하세요.")
        LocalDate endDate) {
}
