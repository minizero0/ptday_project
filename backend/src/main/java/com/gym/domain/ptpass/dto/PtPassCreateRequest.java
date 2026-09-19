package com.gym.domain.ptpass.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gym.domain.ptpass.entity.PtPass;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** PT권 부여 요청. 잔여 횟수는 구매 횟수와 같게 시작하므로 받지 않는다. */
public record PtPassCreateRequest(
        @NotNull(message = "수업 길이를 선택하세요.")
        Integer sessionMinutes,

        @NotNull(message = "PT 횟수를 입력하세요.")
        @Min(value = 1, message = "PT 횟수는 1회 이상이어야 합니다.")
        @Max(value = PtPass.MAX_PT_COUNT, message = "PT 횟수는 " + PtPass.MAX_PT_COUNT + "회 이하여야 합니다.")
        Integer totalCount) {

    @JsonIgnore
    @AssertTrue(message = "수업 길이는 30, 40, 50, 60분 중 하나여야 합니다.")
    public boolean isSessionMinutesAllowed() {
        // null 은 @NotNull 이 따로 알려준다
        return sessionMinutes == null || PtPass.ALLOWED_SESSION_MINUTES.contains(sessionMinutes);
    }
}
