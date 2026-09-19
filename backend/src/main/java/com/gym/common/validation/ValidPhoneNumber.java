package com.gym.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 국내 전화번호 형식 검증. 비어 있는 값은 통과한다(필수 여부는 @NotBlank 로 따로 표현한다).
 * 규칙은 {@link com.gym.common.util.PhoneNumber} 한곳에 있다.
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {

    String message() default "전화번호 형식을 확인하세요. (예: 010-1234-5678)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
