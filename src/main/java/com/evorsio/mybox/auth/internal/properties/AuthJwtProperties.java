package com.evorsio.mybox.auth.internal.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "mybox.auth.jwt")
public class AuthJwtProperties {
    @NotBlank(message = "JWT 密钥不能为空")
    private String secret;
    private Long expiration = 86400L;
    private Long refreshExpiration = 604800L;
    private String issuer = "mybox-server";
    private String audience = "mybox-client";
    private String tokenPrefix = "Bearer";
}
