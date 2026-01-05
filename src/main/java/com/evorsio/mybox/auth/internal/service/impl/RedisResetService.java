package com.evorsio.mybox.auth.internal.service.impl;

import com.evorsio.mybox.auth.EmailService;
import com.evorsio.mybox.auth.PasswordService;
import com.evorsio.mybox.auth.internal.properties.AuthRedisProperties;
import com.evorsio.mybox.auth.internal.repository.AuthRepository;
import com.evorsio.mybox.common.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisResetService implements PasswordService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String,String> redisTemplate;
    private final EmailService emailService;
    private final AuthRedisProperties authRedisProperties;

    @Override
    public void sendPasswordResetToken(String email) {
        String rateKey = RedisKeyConstants.passwordResetRateLimitKey(email);
        Long count = redisTemplate.opsForValue().increment(rateKey);

        // 首次访问，设置过期时间（1小时）
        if(count == 1){
            redisTemplate.expire(rateKey, authRedisProperties.getPasswordResetRateLimitTtlInSeconds(), TimeUnit.SECONDS);
        }

        // 限流检查：超过5次则直接返回
        if(count > 5){
            log.warn("密码重置请求超过限流次数: email={}, count={}", email, count);
            return;
        }

        // 查找用户并发送重置邮件
        authRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            String key = RedisKeyConstants.passwordResetKey(token);

            // 存储 token，15分钟后过期
            redisTemplate.opsForValue().set(
                    key,
                    user.getId().toString(),
                    authRedisProperties.getPasswordResetTtlInSeconds(),
                    TimeUnit.SECONDS
            );

            log.info("发送密码重置邮件: email={}, token={}", email, token);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // 从 Redis 中获取 token 对应的用户 ID
        String key = RedisKeyConstants.passwordResetKey(token);
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            log.warn("密码重置失败：token 无效或已过期: token={}", token);
            throw new IllegalArgumentException("重置令牌无效或已过期");
        }

        try {
            // 查找用户并更新密码
            UUID userUuid = UUID.fromString(userId);
            authRepository.findById(userUuid).ifPresentOrElse(user -> {
                // 更新密码
                user.setPassword(passwordEncoder.encode(newPassword));
                authRepository.save(user);

                // 删除已使用的 token（一次性使用）
                redisTemplate.delete(key);

                log.info("密码重置成功: userId={}, email={}", userUuid, user.getEmail());
            }, () -> {
                log.warn("密码重置失败：用户不存在: userId={}", userId);
                throw new IllegalArgumentException("用户不存在");
            });
        } catch (IllegalArgumentException e) {
            log.error("密码重置失败：无效的用户 ID: userId={}", userId, e);
            throw new IllegalArgumentException("无效的用户 ID");
        }
    }
}
