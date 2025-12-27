package com.evorsio.mybox.auth.service.impl;

import com.evorsio.mybox.auth.domain.TokenType;
import com.evorsio.mybox.auth.domain.User;
import com.evorsio.mybox.auth.domain.UserRole;
import com.evorsio.mybox.auth.dto.TokenResponse;
import com.evorsio.mybox.auth.exception.AuthException;
import com.evorsio.mybox.auth.repository.UserRepository;
import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.auth.service.AuthService;
import com.evorsio.mybox.auth.service.RefreshTokenService;
import com.evorsio.mybox.auth.util.JwtClaimsBuilder;
import com.evorsio.mybox.auth.util.JwtUtil;
import com.evorsio.mybox.common.properties.AuthJwtProperties;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final AuthJwtProperties authJwtProperties;

    @Override
    public TokenResponse register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new AuthException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        UserRole role = userRepository.count() == 0 ? UserRole.ADMIN : UserRole.USER;

        User user = userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build());

        return generateTokenResponse(user);
    }

    @Override
    public TokenResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
        return generateTokenResponse(user);
    }

    @Override
    public boolean validateUser(String username, String password) {
        return userRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

    @Override
    public TokenResponse refreshToken(UUID userId, String refreshToken) {
        if (!refreshTokenService.validateToken(userId, refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        return generateTokenResponse(user);
    }

    @NotNull
    private TokenResponse generateTokenResponse(User user) {
        Map<String, Object> claims = JwtClaimsBuilder.build(user);
        String accessToken = jwtUtil.generateToken(user.getId().toString(), claims, TokenType.ACCESS);
        String refreshToken = jwtUtil.generateToken(user.getId().toString(), null, TokenType.REFRESH);

        refreshTokenService.saveToken(user.getId(), refreshToken);

        return new TokenResponse(
                accessToken,
                authJwtProperties.getTokenPrefix(),
                authJwtProperties.getExpiration(),
                refreshToken
        );
    }
}
