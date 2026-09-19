package com.gym.common.util;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 헬스장 영업 기준의 "오늘". 이용권 만료 같은 날짜 판단은 서버가 도는 곳의 시간대가 아니라
 * 영업 시간대(한국)로 한다. 저장·전송은 UTC 기준이다 (CLAUDE.md §5).
 */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private BusinessTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
