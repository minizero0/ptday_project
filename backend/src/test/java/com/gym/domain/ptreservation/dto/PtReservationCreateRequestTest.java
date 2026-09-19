package com.gym.domain.ptreservation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PtReservationCreateRequestTest {

    private static final Instant START = Instant.parse("2026-09-21T05:10:00Z");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 판매하는_수업_길이면_통과한다() {
        assertThat(validator.validate(new PtReservationCreateRequest(1L, 3L, 50, START))).isEmpty();
    }

    @Test
    void 판매하지_않는_수업_길이는_거부한다() {
        Set<ConstraintViolation<PtReservationCreateRequest>> violations =
                validator.validate(new PtReservationCreateRequest(1L, 3L, 45, START));

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("수업 길이는 30, 40, 50, 60분 중 하나여야 합니다.");
    }

    @Test
    void 수업_길이가_없으면_선택하라는_메시지만_나온다() {
        Set<ConstraintViolation<PtReservationCreateRequest>> violations =
                validator.validate(new PtReservationCreateRequest(1L, 3L, null, START));

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("수업 길이를 선택하세요.");
    }
}
