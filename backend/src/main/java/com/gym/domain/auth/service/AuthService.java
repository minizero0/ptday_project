package com.gym.domain.auth.service;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.account.entity.Account;
import com.gym.domain.account.entity.Role;
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

    /**
     * 입구 키오스크의 토큰 연장. 화면이 켜져 있는 동안 주기적으로 새 토큰을 받아 가므로 로그인이 끊기지 않는다.
     * 발급할 때마다 계정을 다시 확인한다 — 계정을 지우면 다음 연장부터 실패해 그 기기를 끊을 수 있다.
     * 직원 계정은 연장해 주지 않는다(짧은 유효기간이 곧 자리 비움 보호다).
     *
     * @param username 요청 본문이 아니라 현재 토큰에서 온 로그인 아이디
     */
    @Transactional(readOnly = true)
    public LoginResponse refresh(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (account.getRole() != Role.KIOSK) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String accessToken = jwtProvider.createAccessToken(account.getUsername(), account.getRole());
        return new LoginResponse(accessToken, TOKEN_TYPE, account.getUsername(), account.getRole().name());
    }
}
