package com.gym.common.util;

import java.util.regex.Pattern;

/**
 * 국내 전화번호의 검증과 표기 통일.
 * 하이픈 유무와 상관없이 받아 저장 표기를 하나(010-1234-5678)로 맞춘다.
 * 화면(frontend/src/lib/phone.ts)과 같은 규칙이며, 최종 검증 책임은 서버에 있다 (CLAUDE.md §6).
 */
public final class PhoneNumber {

    // 서울 유선(02-123-4567)이 9자리로 가장 짧고 휴대폰이 11자리로 가장 길다
    private static final int MIN_DIGITS = 9;
    private static final int MAX_DIGITS = 11;
    private static final int SEOUL_MAX_DIGITS = 10;

    private static final String SEOUL_AREA_CODE = "02";
    private static final int SEOUL_AREA_CODE_LENGTH = 2;
    private static final int DEFAULT_PREFIX_LENGTH = 3;
    private static final int LAST_GROUP_LENGTH = 4;
    private static final String SEPARATOR = "-";

    // 구분자로 허용하는 것은 하이픈과 공백뿐이다. 문자가 섞인 값을 숫자만 추려 통과시키지 않는다.
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[0-9\\-\\s]+");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D");

    private PhoneNumber() {
    }

    /** 전화번호는 선택 항목이라 비어 있으면 유효하다. */
    public static boolean isValid(String raw) {
        if (isBlank(raw)) {
            return true;
        }
        if (!ALLOWED_CHARACTERS.matcher(raw.trim()).matches()) {
            return false;
        }
        String digits = extractDigits(raw);
        int maxDigits = digits.startsWith(SEOUL_AREA_CODE) ? SEOUL_MAX_DIGITS : MAX_DIGITS;
        return digits.startsWith("0") && digits.length() >= MIN_DIGITS && digits.length() <= maxDigits;
    }

    /**
     * 저장 표기로 통일한다. 비어 있으면 null.
     *
     * @throws IllegalArgumentException 유효하지 않은 값. 잘못된 번호를 그럴듯하게 잘라 저장하는 일을 막는다.
     */
    public static String normalize(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        if (!isValid(raw)) {
            throw new IllegalArgumentException("유효하지 않은 전화번호 형식입니다.");
        }

        String digits = extractDigits(raw);
        int prefixLength = digits.startsWith(SEOUL_AREA_CODE) ? SEOUL_AREA_CODE_LENGTH : DEFAULT_PREFIX_LENGTH;
        int lastGroupStart = digits.length() - LAST_GROUP_LENGTH;

        return String.join(SEPARATOR,
                digits.substring(0, prefixLength),
                digits.substring(prefixLength, lastGroupStart),
                digits.substring(lastGroupStart));
    }

    private static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }

    private static String extractDigits(String raw) {
        return NON_DIGITS.matcher(raw).replaceAll("");
    }
}
