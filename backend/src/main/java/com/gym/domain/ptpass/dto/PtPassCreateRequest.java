package com.gym.domain.ptpass.dto;

import com.gym.domain.ptpass.entity.PtPass;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** PT권 부여 요청. 잔여 횟수는 구매 횟수와 같게 시작하므로 받지 않는다. */
public record PtPassCreateRequest(
        @NotNull(message = "PT 횟수를 입력하세요.")
        @Min(value = 1, message = "PT 횟수는 1회 이상이어야 합니다.")
        @Max(value = PtPass.MAX_PT_COUNT, message = "PT 횟수는 " + PtPass.MAX_PT_COUNT + "회 이하여야 합니다.")
        Integer totalCount) {
}
