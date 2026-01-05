package com.evorsio.mybox.common;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class RedisKeyConstants {

    // ================== 类型名 ==================
    public static final String REDIS_REFRESH_TOKEN = "refreshToken";
    public static final String REDIS_HEARTBEAT = "heartbeat";
    public static final String REDIS_TOKEN_BLACKLIST = "tokenBlacklist";

    // 🔐 密码重置
    public static final String REDIS_PASSWORD_RESET = "passwordReset";
    public static final String REDIS_PASSWORD_RESET_RATE_LIMIT = "passwordResetRateLimit";

    // ================== Key 生成 ==================

    /**
     * RefreshToken
     * mybox:auth:refreshToken:{userId}
     */
    public static String refreshTokenKey(UUID userId) {
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_AUTH,
                REDIS_REFRESH_TOKEN,
                userId.toString()
        );
    }

    /**
     * 心跳
     * mybox:device:heartbeat:{deviceId}
     */
    public static String heartbeatKey(UUID deviceId) {
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_DEVICE,
                REDIS_HEARTBEAT,
                deviceId.toString()
        );
    }

    /**
     * AccessToken 黑名单
     * mybox:auth:tokenBlacklist:{userId}:{tokenHash}
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

    /**
     * 🔐 密码重置 Token
     * mybox:auth:passwordReset:{token}
     */
    public static String passwordResetKey(String token) {
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_AUTH,
                REDIS_PASSWORD_RESET,
                token
        );
    }

    /**
     * 🔐 密码重置请求限流（按邮箱）
     * mybox:auth:passwordResetRateLimit:{email}
     */
    public static String passwordResetRateLimitKey(String email) {
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_AUTH,
                REDIS_PASSWORD_RESET_RATE_LIMIT,
                email.toLowerCase()
        );
    }

    /**
     * 限流（通用）
     * mybox:{module}:rateLimit:{method}
     */
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
