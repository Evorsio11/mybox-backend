package com.evorsio.mybox.auth.service;

import com.evorsio.mybox.auth.dto.TokenResponse;

import java.util.UUID;

public interface AuthService {
    TokenResponse register(String username, String email, String rawPassword);

    TokenResponse login(String username, String rawPassword);

    boolean validateUser(String username, String password);

    TokenResponse refreshToken(UUID userId, String refreshToken);
}
