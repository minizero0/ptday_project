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
 * 앱 시작 시 관리자 계정이 하나도 없으면 .env 값으로 기본 관리자를 생성한다.
 * 이미 존재하면 아무것도 하지 않는다(멱등).
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminAccountInitializer(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountRepository.existsByUsername(adminUsername)) {
            return;
        }
        Account admin = new Account(
                adminUsername,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN,
                null);
        accountRepository.save(admin);
        log.info("기본 관리자 계정을 생성했습니다: username={}", adminUsername);
    }
}
