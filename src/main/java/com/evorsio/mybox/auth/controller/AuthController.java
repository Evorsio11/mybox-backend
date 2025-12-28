package com.evorsio.mybox.auth.controller;

import com.evorsio.mybox.auth.dto.LoginRequest;
import com.evorsio.mybox.auth.dto.RefreshRequest;
import com.evorsio.mybox.auth.dto.RegisterRequest;
import com.evorsio.mybox.auth.dto.TokenResponse;
import com.evorsio.mybox.auth.service.AuthService;
import com.evorsio.mybox.auth.util.JwtUtil;
import com.evorsio.mybox.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request.getUsername(), request.getPassword());
        return ApiResponse.success("登录成功", token);
    }

    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse token = authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );
        return ApiResponse.success("注册成功", token);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        UUID userId = UUID.fromString(jwtUtil.parseToken(refreshToken).getSubject());
        TokenResponse token = authService.refreshToken(userId, refreshToken);
        return ApiResponse.success("刷新令牌成功", token);
    }
}
