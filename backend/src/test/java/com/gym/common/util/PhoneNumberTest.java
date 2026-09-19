package com.gym.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "010-1234-5678",   // 휴대폰, 하이픈 있음
            "01012345678",     // 휴대폰, 하이픈 없음
            "010 1234 5678",   // 공백 구분
            "011-123-4567",    // 옛 휴대폰 10자리
            "02-123-4567",     // 서울 유선 9자리
            "0212345678",      // 서울 유선 10자리
            "031-123-4567",    // 지역 유선 10자리
            "03112345678"      // 지역 유선 11자리
    })
    void 국내_전화번호_형식이면_유효하다(String raw) {
        assertThat(PhoneNumber.isValid(raw)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "010414148894",    // 12자리 — 숫자가 하나 더 들어간 오입력
            "0101234",         // 자릿수 부족
            "1012345678",      // 0 으로 시작하지 않음
            "02123456789",     // 02 는 10자리까지
            "abc01012345678",  // 문자가 섞임 — 조용히 걸러내지 않고 거절한다
            "010-1234-567a",
            "전화없음",
            "+82-10-1234-5678" // 국가번호 표기는 받지 않는다
    })
    void 형식에_맞지_않으면_유효하지_않다(String raw) {
        assertThat(PhoneNumber.isValid(raw)).isFalse();
    }

    @Test
    void 전화번호는_선택_항목이라_비어_있으면_유효하다() {
        assertThat(PhoneNumber.isValid(null)).isTrue();
        assertThat(PhoneNumber.isValid("")).isTrue();
        assertThat(PhoneNumber.isValid("   ")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "01012345678,     010-1234-5678",
            "010-1234-5678,   010-1234-5678",
            "010 1234 5678,   010-1234-5678",
            "0111234567,      011-123-4567",
            "021234567,       02-123-4567",
            "0212345678,      02-1234-5678",
            "0311234567,      031-123-4567",
            "03112345678,     031-1234-5678"
    })
    void 하이픈_유무와_상관없이_같은_표기로_통일한다(String raw, String expected) {
        assertThat(PhoneNumber.normalize(raw)).isEqualTo(expected);
    }

    @Test
    void 앞뒤_공백은_무시하고_통일한다() {
        assertThat(PhoneNumber.normalize("  01012345678  ")).isEqualTo("010-1234-5678");
    }

    @Test
    void 비어_있는_값은_null_로_통일한다() {
        assertThat(PhoneNumber.normalize(null)).isNull();
        assertThat(PhoneNumber.normalize("")).isNull();
        assertThat(PhoneNumber.normalize("   ")).isNull();
    }

    @Test
    void 유효하지_않은_값을_통일하려_하면_예외를_던진다() {
        // DTO 검증을 거친 값만 들어와야 한다. 잘못된 값을 그럴듯한 번호로 바꿔 저장하는 일을 막는다.
        assertThatThrownBy(() -> PhoneNumber.normalize("010414148894"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
