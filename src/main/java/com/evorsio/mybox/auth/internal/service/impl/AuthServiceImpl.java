package com.evorsio.mybox.auth.internal.service.impl;

import com.evorsio.mybox.auth.*;
import com.evorsio.mybox.auth.internal.exception.AuthException;
import com.evorsio.mybox.auth.internal.properties.AuthJwtProperties;
import com.evorsio.mybox.auth.internal.repository.AuthRepository;
import com.evorsio.mybox.auth.internal.util.JwtClaimsBuilder;
import com.evorsio.mybox.auth.internal.util.JwtUtil;
import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.device.DeviceService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final AuthJwtProperties authJwtProperties;
    private final DeviceService deviceService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public void register(String username, String email, String rawPassword, DeviceInfoDto deviceInfo) {
        if (authRepository.existsByUsername(username)) {
            throw new AuthException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (authRepository.existsByEmail(email)) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        UserRole role = authRepository.count() == 0 ? UserRole.ADMIN : UserRole.USER;

        User user = authRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build());

        // 注册成功后直接记录设备信息
        deviceService.registerDevice(user.getId(), deviceInfo);
    }


    @Override
    public TokenResponse login(String username, String rawPassword, DeviceInfoDto deviceInfo) {
        User user = authRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 直接调用deviceService生成deviceToken和deviceId
        var deviceResult = deviceService.loginDeviceAndReturnToken(user.getId(), deviceInfo);
        return generateTokenResponse(user, deviceResult.getDeviceToken(), deviceResult.getDeviceId());
    }

    @Override
    public boolean validateUser(String username, String password) {
        return authRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

    @Override
    public TokenResponse refreshToken(UUID userId, String refreshToken) {
        if (!refreshTokenService.validateToken(userId, refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = authRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        return generateTokenResponse(user, null, null);
    }

    @NotNull
    private TokenResponse generateTokenResponse(User user, String deviceToken, UUID deviceId) {
        Map<String, Object> claims = JwtClaimsBuilder.build(user);
        String accessToken = jwtUtil.generateToken(user.getId().toString(), claims, TokenType.ACCESS);
        String refreshToken = jwtUtil.generateToken(user.getId().toString(), null, TokenType.REFRESH);

        refreshTokenService.saveToken(user.getId(), refreshToken);

        return new TokenResponse(
                accessToken,
                authJwtProperties.getTokenPrefix(),
                authJwtProperties.getExpirationInSeconds(),
                refreshToken,
                deviceToken,
                deviceId
        );
    }

    @Override
    public void logout(UUID userId, String accessToken) {
        log.info("用户 {} 开始登出", userId);

        // 1. 删除 RefreshToken
        refreshTokenService.deleteToken(userId);
        log.debug("用户 {} 的 RefreshToken 已删除", userId);

        // 2. 将 AccessToken 添加到黑名单
        tokenBlacklistService.addToBlacklist(userId, accessToken);
        log.debug("用户 {} 的 AccessToken 已添加到黑名单", userId);

        log.info("用户 {} 登出成功（Token 已撤销）", userId);
    }
}
