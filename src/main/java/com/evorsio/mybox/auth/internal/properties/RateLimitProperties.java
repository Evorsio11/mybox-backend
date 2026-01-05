package com.evorsio.mybox.auth.internal.properties;

import com.evorsio.mybox.common.SizeUnitParser;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置属性
 * <p>
 * 从 application.yml 中读取限流相关配置
 * 支持部署者通过配置文件灵活控制各个接口的限流策略
 */
@Data
@Component
@ConfigurationProperties(prefix = "mybox.auth.rate-limit")
public class RateLimitProperties {

    /**
     * 限流功能总开关
     */
    private boolean enabled = true;

    /**
     * 默认时间窗口，支持格式: 30s, 5m, 1h, 1d
     */
    private String window = "1m";

    /**
     * 默认最大请求次数
     */
    private int maxRequests = 100;

    /**
     * 默认限流对象: global(全局) / ip(IP地址) / user(用户)
     */
    private String scope = "user";

    /**
     * 默认超过限制时的操作: reject(拒绝) / queue(排队) / delay(延迟)
     */
    private String action = "reject";

    /**
     * 各接口的限流策略配置
     */
    private LoginConfig login = new LoginConfig();
    private RegisterConfig register = new RegisterConfig();
    private RefreshConfig refresh = new RefreshConfig();
    private LogoutConfig logout = new LogoutConfig();

    /**
     * 获取时间窗口的秒数
     */
    public int getWindowInSeconds() {
        return SizeUnitParser.parseTimeToSeconds(window);
    }

    /**
     * 登录接口限流配置
     */
    @Data
    public static class LoginConfig {
        private boolean enabled = true;
        private String window = "5m";
        private int maxRequests = 5;
        private String scope = "ip";

        public int getWindowInSeconds() {
            return SizeUnitParser.parseTimeToSeconds(window);
        }
    }

    /**
     * 注册接口限流配置
     */
    @Data
    public static class RegisterConfig {
        private boolean enabled = true;
        private String window = "1h";
        private int maxRequests = 3;
        private String scope = "ip";

        public int getWindowInSeconds() {
            return SizeUnitParser.parseTimeToSeconds(window);
        }
    }

    /**
     * 刷新令牌接口限流配置
     */
    @Data
    public static class RefreshConfig {
        private boolean enabled = true;
        private String window = "1m";
        private int maxRequests = 10;
        private String scope = "user";

        public int getWindowInSeconds() {
            return SizeUnitParser.parseTimeToSeconds(window);
        }
    }

    /**
     * 登出接口限流配置
     */
    @Data
    public static class LogoutConfig {
        private boolean enabled = true;
        private String window = "1m";
        private int maxRequests = 10;
        private String scope = "user";

        public int getWindowInSeconds() {
            return SizeUnitParser.parseTimeToSeconds(window);
        }
    }
}
