package com.gym.domain.trainer.dto;

import com.gym.common.validation.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 트레이너 등록·수정 요청. active 는 수정에서만 의미가 있고, 없으면(null) 그대로 둔다.
 * 등록은 항상 재직 상태로 시작한다.
 */
public record TrainerSaveRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        @ValidPhoneNumber
        String phone,

        Boolean active) {
}
