package com.evorsio.mybox.auth;

import java.util.UUID;

/**
 * Token 黑名单服务接口
 * <p>
 * 用于管理已登出的 Access Token 黑名单
 */
public interface TokenBlacklistService {

    /**
     * 将 Token 添加到黑名单
     *
     * @param userId 用户 ID
     * @param token  Access Token
     */
    void addToBlacklist(UUID userId, String token);

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param userId 用户 ID
     * @param token  Access Token
     * @return true-在黑名单中，false-不在黑名单中
     */
    boolean isBlacklisted(UUID userId, String token);

    /**
     * 从黑名单中移除 Token
     *
     * @param userId 用户 ID
     * @param token  Access Token
     */
    void removeFromBlacklist(UUID userId, String token);
}
