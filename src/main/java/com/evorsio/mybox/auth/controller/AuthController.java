package com.evorsio.mybox.auth.controller;

import com.evorsio.mybox.auth.dto.LoginRequest;
import com.evorsio.mybox.auth.dto.RefreshRequest;
import com.evorsio.mybox.auth.dto.RegisterRequest;
import com.evorsio.mybox.auth.dto.TokenResponse;
import com.evorsio.mybox.auth.service.AuthService;
import com.evorsio.mybox.auth.util.JwtUtil;
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
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/register")
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        UUID userId = UUID.fromString(jwtUtil.parseToken(refreshToken).getSubject());
        return authService.refreshToken(userId, refreshToken);
    }
}
