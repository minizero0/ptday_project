package com.gym.domain.auth.service;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.account.entity.Account;
import com.gym.domain.account.repository.AccountRepository;
import com.gym.domain.auth.dto.LoginRequest;
import com.gym.domain.auth.dto.LoginResponse;
import com.gym.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 계정 없음/비밀번호 불일치를 구분하지 않고 동일한 실패로 처리한다(정보 노출 방지)
        Account account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        String accessToken = jwtProvider.createAccessToken(account.getUsername(), account.getRole());
        return new LoginResponse(accessToken, TOKEN_TYPE, account.getUsername(), account.getRole().name());
    }
}
