package com.gym.domain.ptpass.exception;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;

/** 잔여 횟수보다 많이 차감하려 할 때. 잔여 횟수는 어떤 경로로도 음수가 되지 않는다 (CLAUDE.md §9). */
public class InsufficientPtCountException extends BusinessException {

    public InsufficientPtCountException() {
        super(ErrorCode.INSUFFICIENT_PT_COUNT);
    }
}
