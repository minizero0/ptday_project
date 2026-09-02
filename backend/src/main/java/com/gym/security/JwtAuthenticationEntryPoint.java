package com.gym.security;

import com.gym.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 인증되지 않은 요청에 대해 기본 403 대신 401 + 통일 응답 래퍼로 반환한다 (CLAUDE.md §5, §7).
 * 고정 형태의 응답이라 별도 직렬화 없이 JSON 을 직접 작성한다.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = String.format(
                "{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}",
                errorCode.name(), errorCode.getMessage());
        response.getWriter().write(body);
    }
}
