package com.evorsio.mybox.auth.internal.service.impl;

import com.evorsio.mybox.auth.TokenBlacklistService;
import com.evorsio.mybox.common.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务实现
 * <p>
 * 使用 Redis 存储 Token 黑名单，支持过期时间自动清理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void addToBlacklist(UUID userId, String token) {
        String key = RedisKeyConstants.tokenBlacklistKey(userId, token);
        String value = String.valueOf(System.currentTimeMillis());

        // 存储到 Redis，设置 24 小时过期时间
        redisTemplate.opsForValue().set(
                key,
                value,
                RedisKeyConstants.TOKEN_BLACKLIST_TTL,
                TimeUnit.SECONDS
        );

        log.debug("Token 已添加到黑名单: userId={}, key={}", userId, key);
    }

    @Override
    public boolean isBlacklisted(UUID userId, String token) {
        String key = RedisKeyConstants.tokenBlacklistKey(userId, token);
        Boolean exists = redisTemplate.hasKey(key);

        if (exists) {
            log.debug("Token 在黑名单中: userId={}, key={}", userId, key);
            return true;
        }

        return false;
    }

    @Override
    public void removeFromBlacklist(UUID userId, String token) {
        String key = RedisKeyConstants.tokenBlacklistKey(userId, token);
        redisTemplate.delete(key);

        log.debug("Token 已从黑名单移除: userId={}, key={}", userId, key);
    }
}
