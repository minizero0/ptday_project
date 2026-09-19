package com.gym.domain.ptpass.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PtPassCreateRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void 판매하는_수업_길이와_횟수면_유효하다() {
        assertThat(validator.validate(new PtPassCreateRequest(30, 10))).isEmpty();
        assertThat(validator.validate(new PtPassCreateRequest(60, 1))).isEmpty();
    }

    @Test
    void 판매하지_않는_수업_길이는_거절한다() {
        assertThat(messages(new PtPassCreateRequest(45, 10)))
                .containsExactly("수업 길이는 30, 40, 50, 60분 중 하나여야 합니다.");
    }

    @Test
    void 수업_길이가_없으면_그_사실만_알린다() {
        assertThat(messages(new PtPassCreateRequest(null, 10))).containsExactly("수업 길이를 선택하세요.");
    }

    private static Set<String> messages(PtPassCreateRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
