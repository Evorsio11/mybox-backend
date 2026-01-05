package com.evorsio.mybox.common;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class RedisKeyConstants {

    // Redis 类型名
    public static final String REDIS_REFRESH_TOKEN = "refreshToken";
    public static final String REDIS_HEARTBEAT = "heartbeat";
    public static final String REDIS_TOKEN_BLACKLIST = "tokenBlacklist";

    // Access Token 黑名单过期时间（24小时）
    public static final long TOKEN_BLACKLIST_TTL = 24 * 60 * 60;


    //格式：<project>:<module>:<type>:<userId>
    public static String refreshTokenKey(UUID userId) {
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_AUTH,
                REDIS_REFRESH_TOKEN,
                userId.toString()
        );
    }

    public static String heartbeatKey(UUID deviceId) {
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_DEVICE,
                REDIS_HEARTBEAT,
                deviceId.toString()
        );
    }

    /**
     * 生成 Token 黑名单键
     * 格式：mybox:auth:tokenBlacklist:{userId}:{tokenHash}
     * 使用 token 的哈希值避免键过长
     */
    public static String tokenBlacklistKey(UUID userId, String token) {
        String tokenHash = String.valueOf(Math.abs(token.hashCode()));
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_AUTH,
                REDIS_TOKEN_BLACKLIST,
                userId.toString(),
                tokenHash
        );
    }

    public static String rateLimitKey(String module, String methodName) {
        return composeKey(
                MyboxConstants.PROJECT,
                module,
                "rateLimit",
                methodName
        );
    }

    private static String composeKey(String... parts) {
        return String.join(":", parts);
    }
}
