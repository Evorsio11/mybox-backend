package com.evorsio.mybox.auth.service.impl;

import com.evorsio.mybox.auth.domain.TokenType;
import com.evorsio.mybox.auth.domain.User;
import com.evorsio.mybox.auth.domain.UserRole;
import com.evorsio.mybox.auth.dto.DeviceInfoDto;
import com.evorsio.mybox.auth.dto.TokenResponse;
import com.evorsio.mybox.auth.exception.AuthException;
import com.evorsio.mybox.auth.properties.AuthJwtProperties;
import com.evorsio.mybox.auth.repository.AuthRepository;
import com.evorsio.mybox.auth.service.AuthService;
import com.evorsio.mybox.auth.service.RefreshTokenService;
import com.evorsio.mybox.auth.util.JwtClaimsBuilder;
import com.evorsio.mybox.auth.util.JwtUtil;
import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.event.UserLoggedInEvent;
import com.evorsio.mybox.common.event.UserRegisteredEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        // 注册成功后记录设备信息
        publishRegisterEvent(deviceInfo, user);
    }


    @Override
    public TokenResponse login(String username, String rawPassword, DeviceInfoDto deviceInfo) {
        User user = authRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 发布事件并获取 event
        UserLoggedInEvent event = publishLoginEvent(deviceInfo, user);

        // 阻塞等待 deviceToken 生成
        String deviceToken = event.getDeviceTokenFuture().join();
        return  generateTokenResponse(user,deviceToken);
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
        return generateTokenResponse(user, null);
    }

    @NotNull
    private TokenResponse generateTokenResponse(User user,String deviceToken) {
        Map<String, Object> claims = JwtClaimsBuilder.build(user);
        String accessToken = jwtUtil.generateToken(user.getId().toString(), claims, TokenType.ACCESS);
        String refreshToken = jwtUtil.generateToken(user.getId().toString(), null, TokenType.REFRESH);

        refreshTokenService.saveToken(user.getId(), refreshToken);

        return new TokenResponse(
                accessToken,
                authJwtProperties.getTokenPrefix(),
                authJwtProperties.getExpiration(),
                refreshToken,
                deviceToken
        );
    }

    private UserLoggedInEvent publishLoginEvent(DeviceInfoDto deviceInfo, User user) {
        UserLoggedInEvent event = new UserLoggedInEvent(
                this,
                user.getId(),
                deviceInfo.getDeviceId(),
                deviceInfo.getDeviceName(),
                deviceInfo.getDeviceType(),
                deviceInfo.getOsName(),
                deviceInfo.getOsVersion()
        );

        eventPublisher.publishEvent(event);
        return event;
    }


    private void publishRegisterEvent(DeviceInfoDto deviceInfo, User user) {
        eventPublisher.publishEvent(
                new UserRegisteredEvent(
                        this,
                        user.getId(),
                        deviceInfo.getDeviceId(),
                        deviceInfo.getDeviceName(),
                        deviceInfo.getDeviceType(),
                        deviceInfo.getOsName(),
                        deviceInfo.getOsVersion()
                )
        );
    }
}
