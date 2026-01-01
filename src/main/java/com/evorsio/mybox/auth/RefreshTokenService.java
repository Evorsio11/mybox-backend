package com.evorsio.mybox.auth;

import java.util.UUID;

public interface RefreshTokenService {
    void saveToken(UUID userId, String refreshToken);

    String getToken(UUID userId);

    void deleteToken(UUID userId);

    boolean validateToken(UUID userId, String refreshToken);

    String buildKey(UUID userId);
}
