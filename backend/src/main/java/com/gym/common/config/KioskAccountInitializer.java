package com.gym.common.config;

import com.gym.domain.account.entity.Account;
import com.gym.domain.account.entity.Role;
import com.gym.domain.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 .env 에 키오스크 계정 값이 있고 아직 계정이 없으면 출석 전용(KIOSK) 계정을 만든다.
 * 키오스크를 쓰지 않는 환경을 위해 값이 비어 있으면 아무것도 하지 않는다. 이미 존재해도 마찬가지다(멱등).
 */
@Component
public class KioskAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KioskAccountInitializer.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String kioskUsername;
    private final String kioskPassword;

    public KioskAccountInitializer(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.kiosk.username}") String kioskUsername,
            @Value("${app.kiosk.password}") String kioskPassword) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.kioskUsername = kioskUsername;
        this.kioskPassword = kioskPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (kioskUsername.isBlank() || kioskPassword.isBlank()) {
            return;
        }
        if (accountRepository.existsByUsername(kioskUsername)) {
            return;
        }

        Account kiosk = new Account(
                kioskUsername,
                passwordEncoder.encode(kioskPassword),
                Role.KIOSK,
                null);
        accountRepository.save(kiosk);
        log.info("출석 키오스크 계정을 생성했습니다: username={}", kioskUsername);
    }
}
