package com.gym.domain.auth.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.auth.dto.LoginRequest;
import com.gym.domain.auth.dto.LoginResponse;
import com.gym.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 키오스크 토큰 연장. 누구의 토큰을 연장할지는 본문이 아니라 현재 인증 정보에서 가져온다. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(Authentication authentication) {
        LoginResponse response = authService.refresh(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
