package com.evorsio.mybox.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 速率限制注解
 * <p>
 * 用于限制接口请求频率，简化配置，更加用户友好
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 自定义限流 key
     * <p>
     * 默认为空，自动使用 module:methodName 作为 key
     */
    String key() default "";

    /**
     * 时间窗口，支持格式: 30s, 5m, 1h, 1d
     * <p>
     * 默认为空，使用全局配置
     */
    String window() default "";

    /**
     * 最大请求次数
     * <p>
     * 默认为 0，使用全局配置
     */
    int maxRequests() default 0;

    /**
     * 限流对象
     * <p>
     * 默认为 DEFAULT，使用全局配置
     */
    Scope scope() default Scope.DEFAULT;

    /**
     * 超过限制时的操作
     * <p>
     * 默认为 DEFAULT，使用全局配置
     */
    Action action() default Action.DEFAULT;

    /**
     * 限流对象枚举
     */
    enum Scope {
        /**
         * 默认：使用全局配置
         */
        DEFAULT,

        /**
         * 全局限流
         */
        GLOBAL,

        /**
         * 按 IP 限流
         */
        IP,

        /**
         * 按用户限流（需要 @CurrentUser 参数）
         */
        USER
    }

    /**
     * 限流操作枚举
     */
    enum Action {
        /**
         * 默认：使用全局配置
         */
        DEFAULT,

        /**
         * 拒绝请求（抛出异常）
         */
        REJECT,

        /**
         * 排队等待（暂未实现）
         */
        QUEUE,

        /**
         * 延迟处理（暂未实现）
         */
        DELAY
    }
}
