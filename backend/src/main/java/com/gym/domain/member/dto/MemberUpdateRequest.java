package com.gym.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 회원 정보 수정 요청. member_no 등 불변 값은 받지 않는다.
 */
public record MemberUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phone,

        @Size(max = 10)
        String gender,

        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate) {
}
