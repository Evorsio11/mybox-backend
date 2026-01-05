package com.evorsio.mybox.auth.internal.properties;

import com.evorsio.mybox.common.SizeUnitParser;
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
    private String expiration = "1d";
    private String refreshExpiration = "7d";
    private String issuer = "mybox-server";
    private String audience = "mybox-client";
    private String tokenPrefix = "Bearer";

    /**
     * 获取访问令牌过期时间（秒）
     */
    public long getExpirationInSeconds() {
        return SizeUnitParser.parseTimeToSeconds(expiration);
    }

    /**
     * 获取刷新令牌过期时间（秒）
     */
    public long getRefreshExpirationInSeconds() {
        return SizeUnitParser.parseTimeToSeconds(refreshExpiration);
    }
}
