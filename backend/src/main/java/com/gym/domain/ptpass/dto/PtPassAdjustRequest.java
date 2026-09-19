package com.gym.domain.ptpass.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gym.domain.ptpass.entity.PtPass;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PT 횟수 수동 조정 요청. 더할 때는 양수, 뺄 때는 음수.
 * 처리자는 받지 않는다 — 서버가 로그인 정보에서 채운다.
 */
public record PtPassAdjustRequest(
        @NotNull(message = "증감 횟수를 입력하세요.")
        @Min(value = -PtPass.MAX_PT_COUNT, message = "증감 횟수가 너무 작습니다.")
        @Max(value = PtPass.MAX_PT_COUNT, message = "증감 횟수가 너무 큽니다.")
        Integer delta,

        @NotBlank(message = "사유를 입력하세요.")
        @Size(max = 200, message = "사유는 200자 이하여야 합니다.")
        String reason) {

    @JsonIgnore
    @AssertTrue(message = "증감 횟수는 0일 수 없습니다.")
    public boolean isDeltaNonZero() {
        // null 은 @NotNull 이 따로 알려준다
        return delta == null || delta != 0;
    }
}
