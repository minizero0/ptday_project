package com.gym.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.account.entity.Account;
import com.gym.domain.account.entity.Role;
import com.gym.domain.account.repository.AccountRepository;
import com.gym.domain.auth.dto.LoginResponse;
import com.gym.security.JwtProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String KIOSK = "kiosk";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void 키오스크_계정은_새_토큰으로_연장된다() {
        when(accountRepository.findByUsername(KIOSK))
                .thenReturn(Optional.of(new Account(KIOSK, "hash", Role.KIOSK, null)));
        when(jwtProvider.createAccessToken(KIOSK, Role.KIOSK)).thenReturn("new-token");

        LoginResponse response = authService.refresh(KIOSK);

        assertThat(response.accessToken()).isEqualTo("new-token");
        assertThat(response.username()).isEqualTo(KIOSK);
        assertThat(response.role()).isEqualTo("KIOSK");
    }

    @Test
    void 계정이_지워졌으면_토큰이_살아_있어도_연장되지_않는다() {
        when(accountRepository.findByUsername(KIOSK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(KIOSK))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
        verify(jwtProvider, never()).createAccessToken(any(), any());
    }

    @Test
    void 키오스크가_아닌_계정은_연장해_주지_않는다() {
        when(accountRepository.findByUsername("admin"))
                .thenReturn(Optional.of(new Account("admin", "hash", Role.ADMIN, null)));

        assertThatThrownBy(() -> authService.refresh("admin"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(jwtProvider, never()).createAccessToken(any(), any());
    }
}
