package com.evorsio.mybox.auth;

import java.util.UUID;

public interface AuthService {
    void register(String username, String email, String rawPassword, DeviceInfoDto deviceInfo);

    TokenResponse login(String username, String rawPassword, DeviceInfoDto deviceInfo);

    boolean validateUser(String username, String password);

    TokenResponse refreshToken(UUID userId, String refreshToken);

    /**
     * 用户登出
     * <p>
     * 删除 RefreshToken 并将 AccessToken 加入黑名单，立即撤销访问权限
     *
     * @param userId      用户 ID
     * @param accessToken 当前使用的 Access Token（从 Authorization 头中提取）
     */
    void logout(UUID userId, String accessToken);
}
