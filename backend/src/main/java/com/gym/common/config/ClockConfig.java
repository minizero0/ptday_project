package com.gym.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** "지금"을 주입받게 해 시간에 기대는 규칙(지난 시각 예약 금지 등)을 테스트에서 고정할 수 있게 한다. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
