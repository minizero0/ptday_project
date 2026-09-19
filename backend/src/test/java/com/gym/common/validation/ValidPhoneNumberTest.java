package com.gym.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidPhoneNumberTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    // 실제 요청 DTO 와 같은 모양(record 필드에 애노테이션)으로 검증한다
    private record PhoneHolder(@ValidPhoneNumber String phone) {
    }

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
    void 형식에_맞는_전화번호는_위반이_없다() {
        assertThat(validator.validate(new PhoneHolder("01012345678"))).isEmpty();
        assertThat(validator.validate(new PhoneHolder("02-123-4567"))).isEmpty();
    }

    @Test
    void 선택_항목이라_비어_있어도_위반이_없다() {
        assertThat(validator.validate(new PhoneHolder(null))).isEmpty();
        assertThat(validator.validate(new PhoneHolder(""))).isEmpty();
    }

    @Test
    void 형식에_맞지_않으면_사용자에게_보여줄_메시지와_함께_위반이_난다() {
        Set<ConstraintViolation<PhoneHolder>> violations =
                validator.validate(new PhoneHolder("010414148894"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("전화번호 형식을 확인하세요. (예: 010-1234-5678)");
    }
}
