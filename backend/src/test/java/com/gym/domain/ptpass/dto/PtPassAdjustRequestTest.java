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

class PtPassAdjustRequestTest {

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
    void 더하거나_빼는_횟수와_사유가_있으면_유효하다() {
        assertThat(validator.validate(new PtPassAdjustRequest(2, "서비스 2회 추가"))).isEmpty();
        assertThat(validator.validate(new PtPassAdjustRequest(-1, "잘못 부여한 1회 정정"))).isEmpty();
    }

    @Test
    void 증감이_0이면_거절한다() {
        assertThat(messages(new PtPassAdjustRequest(0, "사유"))).containsExactly("증감 횟수는 0일 수 없습니다.");
    }

    @Test
    void 사유가_없으면_거절한다() {
        assertThat(messages(new PtPassAdjustRequest(1, "   "))).containsExactly("사유를 입력하세요.");
    }

    @Test
    void 증감_횟수가_없으면_그_사실만_알린다() {
        // null 일 때 "0일 수 없습니다"까지 함께 뜨면 안 된다
        assertThat(messages(new PtPassAdjustRequest(null, "사유"))).containsExactly("증감 횟수를 입력하세요.");
    }

    private static Set<String> messages(PtPassAdjustRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
