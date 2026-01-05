package com.evorsio.mybox.device.internal.service;

import com.evorsio.mybox.common.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 设备心跳 Redis 服务
 * 只存储心跳时间，在线状态由 Domain 层根据心跳时间计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceOnlineStatusService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 键过期时间：10分钟（心跳间隔5分钟，留余量）
    private static final Duration KEY_TTL = Duration.ofMinutes(10);

    /**
     * 记录设备心跳
     *
     * @param deviceId 设备 ID
     */
    public void recordHeartbeat(UUID deviceId) {
        String key = RedisKeyConstants.heartbeatKey(deviceId);
        String value = LocalDateTime.now().toString();

        redisTemplate.opsForValue().set(key, value, KEY_TTL.getSeconds(), TimeUnit.SECONDS);
        log.debug("记录设备心跳: deviceId={}, time={}", deviceId, value);
    }

    /**
     * 获取设备最后心跳时间
     *
     * @param deviceId 设备 ID
     * @return 最后心跳时间，如果不存在返回 null
     */
    public LocalDateTime getLastHeartbeat(UUID deviceId) {
        String key = RedisKeyConstants.heartbeatKey(deviceId);
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value.toString());
    }

    /**
     * 删除设备心跳记录（设备登出时调用）
     *
     * @param deviceId 设备 ID
     */
    public void removeHeartbeat(UUID deviceId) {
        String key = RedisKeyConstants.heartbeatKey(deviceId);
        redisTemplate.delete(key);
        log.info("删除设备心跳记录: deviceId={}", deviceId);
    }
}
