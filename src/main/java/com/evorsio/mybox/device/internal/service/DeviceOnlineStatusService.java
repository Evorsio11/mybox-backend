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
 * 设备在线状态 Redis 服务
 * 负责存储设备的瞬时态数据：在线状态、心跳时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceOnlineStatusService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_STATUS_KEY_PREFIX = "device:online:";
    private static final String HEARTBEAT_KEY_PREFIX = "device:heartbeat:";
    // Redis 键过期时间：10分钟（心跳间隔5分钟，留余量）
    private static final Duration KEY_TTL = Duration.ofMinutes(10);

    /**
     * 记录设备心跳
     *
     * @param deviceId 设备 ID
     */
    public void recordHeartbeat(UUID deviceId) {
        String key = HEARTBEAT_KEY_PREFIX + deviceId;
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
        String key = HEARTBEAT_KEY_PREFIX + deviceId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value.toString());
    }

    /**
     * 设置设备在线状态
     *
     * @param deviceId 设备 ID
     * @param status   在线状态
     */
    public void setOnlineStatus(UUID deviceId, com.evorsio.mybox.device.OnlineStatus status) {
        String key = ONLINE_STATUS_KEY_PREFIX + deviceId;
        redisTemplate.opsForValue().set(key, status.name(), KEY_TTL.getSeconds(), TimeUnit.SECONDS);
        log.debug("设置设备在线状态: deviceId={}, status={}", deviceId, status);
    }

    /**
     * 获取设备在线状态
     *
     * @param deviceId 设备 ID
     * @return 在线状态，如果不存在返回 OFFLINE
     */
    public com.evorsio.mybox.device.OnlineStatus getOnlineStatus(UUID deviceId) {
        String key = ONLINE_STATUS_KEY_PREFIX + deviceId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return com.evorsio.mybox.device.OnlineStatus.OFFLINE;
        }

        return com.evorsio.mybox.device.OnlineStatus.valueOf(value.toString());
    }

    /**
     * 删除设备在线状态（设备登出时调用）
     *
     * @param deviceId 设备 ID
     */
    public void removeOnlineStatus(UUID deviceId) {
        String onlineKey = ONLINE_STATUS_KEY_PREFIX + deviceId;
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + deviceId;

        redisTemplate.delete(onlineKey);
        redisTemplate.delete(heartbeatKey);
        log.info("删除设备在线状态: deviceId={}", deviceId);
    }

    /**
     * 清理过期的在线状态（定时任务调用）
     * Redis 会自动过期，此方法可选，用于手动触发清理
     */
    public void cleanupExpiredStatus() {
        // Redis TTL 会自动清理过期键，此方法保留用于可能的批量清理场景
        log.debug("检查设备在线状态 TTL（Redis 自动处理）");
    }
}
