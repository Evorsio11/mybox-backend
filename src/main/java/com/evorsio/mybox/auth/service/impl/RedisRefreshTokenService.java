package com.evorsio.mybox.auth.service.impl;

import com.evorsio.mybox.auth.service.RefreshTokenService;
import com.evorsio.mybox.common.constants.RedisKeyConstants;
import com.evorsio.mybox.common.properties.AuthJwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenService implements RefreshTokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthJwtProperties authJwtProperties;

    @Override
    public void saveToken(UUID userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                buildKey(userId),
                refreshToken,
                authJwtProperties.getRefreshExpiration(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public String getToken(UUID userId) {
        return redisTemplate.opsForValue().get(buildKey(userId));
    }

    @Override
    public void deleteToken(UUID userId) {
        redisTemplate.delete(buildKey(userId));
    }

    @Override
    public boolean validateToken(UUID userId, String refreshToken) {
        String storedToken = getToken(userId);
        return refreshToken != null && refreshToken.equals(storedToken);
    }

    @Override
    public String buildKey(UUID userId) {
        return RedisKeyConstants.refreshTokenKey(userId);
    }
}
