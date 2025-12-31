package com.evorsio.mybox.auth.service;

import com.evorsio.mybox.auth.dto.DeviceInfoDto;
import com.evorsio.mybox.auth.dto.TokenResponse;

import java.util.UUID;

public interface AuthService {
    void register(String username, String email, String rawPassword, DeviceInfoDto deviceInfo);

    TokenResponse login(String username, String rawPassword, DeviceInfoDto deviceInfo);

    boolean validateUser(String username, String password);

    TokenResponse refreshToken(UUID userId, String refreshToken);
}
