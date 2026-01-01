package com.evorsio.mybox.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String refreshToken;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String deviceToken;
}
