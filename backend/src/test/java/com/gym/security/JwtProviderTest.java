package com.gym.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.gym.domain.account.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    // 테스트용 비밀키 (HS256 최소 32바이트)
    private static final String SECRET = "test_secret_key_for_jwt_provider_unit_test_0123456789";
    private static final long VALIDITY_SECONDS = 3600;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, VALIDITY_SECONDS);
    }

    @Test
    void 토큰을_발급하고_파싱하면_원래_값이_보존된다() {
        String token = jwtProvider.createAccessToken("admin", Role.ADMIN);

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUsername(token)).isEqualTo("admin");
        assertThat(jwtProvider.getRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void 변조되거나_잘못된_토큰은_유효하지_않다() {
        assertThat(jwtProvider.validateToken("not-a-jwt")).isFalse();

        // payload 를 변조하면 서명이 맞지 않아 검증에 실패해야 한다
        String token = jwtProvider.createAccessToken("admin", Role.ADMIN);
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[0] = (payload[0] == 'A') ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];
        assertThat(jwtProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void 다른_비밀키로_검증하면_실패한다() {
        String token = jwtProvider.createAccessToken("admin", Role.ADMIN);

        JwtProvider otherProvider =
                new JwtProvider("completely_different_secret_key_for_test_9876543210", VALIDITY_SECONDS);
        assertThat(otherProvider.validateToken(token)).isFalse();
    }
}
