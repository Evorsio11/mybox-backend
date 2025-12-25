package com.evorsio.mybox.auth.controller;

import com.evorsio.mybox.auth.domain.TokenType;
import com.evorsio.mybox.auth.domain.User;
import com.evorsio.mybox.auth.dto.LoginRequest;
import com.evorsio.mybox.auth.dto.RegisterRequest;
import com.evorsio.mybox.auth.dto.TokenResponse;
import com.evorsio.mybox.auth.exception.InvalidCredentialsException;
import com.evorsio.mybox.auth.service.AuthService;
import com.evorsio.mybox.auth.util.JwtClaimsBuilder;
import com.evorsio.mybox.auth.util.JwtUtil;
import com.evorsio.mybox.common.properties.AuthJwtProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthJwtProperties authJwtProperties;

    @PostMapping("login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            throw new InvalidCredentialsException();
        }
        return getTokenResponse(user);
    }

    @PostMapping("register")
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return getTokenResponse(user);
    }

    @NotNull
    private TokenResponse getTokenResponse(User user) {
        Map<String, Object> claims = JwtClaimsBuilder.build(user);
        String accessToken = jwtUtil.generateToken(user.getUsername(), claims, TokenType.ACCESS);
        String refreshToken = jwtUtil.generateToken(user.getUsername(), null, TokenType.REFRESH);

        return new TokenResponse(
                accessToken,
                authJwtProperties.getTokenPrefix(),
                authJwtProperties.getExpiration(),
                refreshToken
        );
    }

}
