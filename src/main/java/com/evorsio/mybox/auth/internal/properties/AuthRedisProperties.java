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
@ConfigurationProperties(prefix = "mybox.auth.redis")
public class AuthRedisProperties {

    @NotBlank(message = "Token 黑名单 TTL 不能为空")
    private String tokenBlacklistTtl = "24h";

    @NotBlank(message = "密码重置 Token TTL 不能为空")
    private String passwordResetTtl = "15m";

    @NotBlank(message = "密码重置限流 TTL 不能为空")
    private String passwordResetRateLimitTtl = "1h";

    /**
     * 获取 Token 黑名单过期时间（秒）
     */
    public long getTokenBlacklistTtlInSeconds() {
        return SizeUnitParser.parseTimeToSeconds(tokenBlacklistTtl);
    }

    /**
     * 获取密码重置 Token 过期时间（秒）
     */
    public long getPasswordResetTtlInSeconds() {
        return SizeUnitParser.parseTimeToSeconds(passwordResetTtl);
    }

    /**
     * 获取密码重置请求限流过期时间（秒）
     */
    public long getPasswordResetRateLimitTtlInSeconds() {
        return SizeUnitParser.parseTimeToSeconds(passwordResetRateLimitTtl);
    }
}
