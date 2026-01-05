package com.evorsio.mybox.auth.internal.aspect;

import com.evorsio.mybox.auth.CurrentUser;
import com.evorsio.mybox.auth.RateLimit;
import com.evorsio.mybox.auth.UserPrincipal;
import com.evorsio.mybox.auth.internal.properties.RateLimitProperties;
import com.evorsio.mybox.auth.internal.exception.RateLimitException;
import com.evorsio.mybox.common.RedisKeyConstants;
import com.evorsio.mybox.common.SizeUnitParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;

/**
 * 限流切面
 * <p>
 * 用户友好的限流配置，支持多种限流策略
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitProperties rateLimitProperties;
    private final RedisScript<Boolean> rateLimitScript;

    /**
     * 构造函数
     */
    public RateLimitAspect(
            RedisTemplate<String, Object> redisTemplate,
            RateLimitProperties rateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;

        // 初始化 Lua 脚本
        String luaScript = """
                local c = redis.call('INCR', KEYS[1])
                if c == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                if c > tonumber(ARGV[2]) then
                    return 0
                else
                    return 1
                end
                """;
        this.rateLimitScript = RedisScript.of(luaScript, Boolean.class);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 检查限流功能是否启用
        if (!rateLimitProperties.isEnabled()) {
            log.debug("限流功能已禁用，跳过限流检查");
            return joinPoint.proceed();
        }

        // 获取方法名，用于查找特定配置
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();

        // 根据方法名获取特定配置（如果注解中没有指定参数）
        RateLimitConfig config = getConfigForMethod(methodName, rateLimit);

        // 检查该接口的限流是否启用
        if (!config.enabled) {
            log.debug("方法 {} 的限流已禁用，跳过限流检查", methodName);
            return joinPoint.proceed();
        }

        // 构建 Redis key
        String key = buildKey(joinPoint, rateLimit, config);

        // 获取时间窗口（秒）
        int windowInSeconds = config.windowInSeconds;

        // 获取最大请求次数
        int maxRequests = config.maxRequests;

        // 获取限流操作
        RateLimit.Action action = getAction(rateLimit);

        // 执行限流检查
        List<String> keys = Collections.singletonList(key);
        Boolean allowed = redisTemplate.execute(
                rateLimitScript,
                keys,
                String.valueOf(windowInSeconds),
                String.valueOf(maxRequests)
        );

        // 根据限流结果处理
        if (Boolean.FALSE.equals(allowed)) {
            log.warn("达到速率限制上限: {}, 时间窗口: {}秒, 最大请求次数: {}",
                    key, windowInSeconds, maxRequests);

            // 根据不同的 action 处理
            return handleRateLimitExceeded(action, joinPoint, rateLimit);
        }

        return joinPoint.proceed();
    }

    /**
     * 构建 Redis key
     */
    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit, RateLimitConfig config) {
        // 如果注解中指定了 key，直接使用
        if (!rateLimit.key().isEmpty()) {
            return rateLimit.key();
        }

        // 否则使用默认格式：rateLimit:{module}:{methodName}[:scope]
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        String module = extractModule(joinPoint);

        StringBuilder keyBuilder = new StringBuilder(RedisKeyConstants.rateLimitKey(module, methodName));

        // 根据限流对象添加后缀
        String scopeSuffix = switch (config.scope) {
            case "ip" -> ":ip:" + getClientIp();
            case "user" -> ":user:" + extractUserId(joinPoint);
            case "global" -> ":global";
            default -> "";
        };

        keyBuilder.append(scopeSuffix);
        return keyBuilder.toString();
    }

    /**
     * 根据方法名获取配置
     */
    private RateLimitConfig getConfigForMethod(String methodName, RateLimit rateLimit) {
        RateLimitConfig config = new RateLimitConfig();

        // 根据方法名选择配置
        switch (methodName) {
            case "login" -> {
                config.enabled = rateLimitProperties.getLogin().isEnabled();
                config.windowInSeconds = rateLimitProperties.getLogin().getWindowInSeconds();
                config.maxRequests = rateLimitProperties.getLogin().getMaxRequests();
                config.scope = rateLimitProperties.getLogin().getScope();
            }
            case "register" -> {
                config.enabled = rateLimitProperties.getRegister().isEnabled();
                config.windowInSeconds = rateLimitProperties.getRegister().getWindowInSeconds();
                config.maxRequests = rateLimitProperties.getRegister().getMaxRequests();
                config.scope = rateLimitProperties.getRegister().getScope();
            }
            case "refresh" -> {
                config.enabled = rateLimitProperties.getRefresh().isEnabled();
                config.windowInSeconds = rateLimitProperties.getRefresh().getWindowInSeconds();
                config.maxRequests = rateLimitProperties.getRefresh().getMaxRequests();
                config.scope = rateLimitProperties.getRefresh().getScope();
            }
            case "logout" -> {
                config.enabled = rateLimitProperties.getLogout().isEnabled();
                config.windowInSeconds = rateLimitProperties.getLogout().getWindowInSeconds();
                config.maxRequests = rateLimitProperties.getLogout().getMaxRequests();
                config.scope = rateLimitProperties.getLogout().getScope();
            }
            default -> {
                // 默认使用全局配置
                config.enabled = true;
                config.windowInSeconds = rateLimitProperties.getWindowInSeconds();
                config.maxRequests = rateLimitProperties.getMaxRequests();
                config.scope = rateLimitProperties.getScope();
            }
        }

        // 如果注解中指定了参数，优先使用注解的值
        if (rateLimit.maxRequests() > 0) {
            config.maxRequests = rateLimit.maxRequests();
        }
        if (!rateLimit.window().isEmpty()) {
            config.windowInSeconds = SizeUnitParser.parseTimeToSeconds(rateLimit.window());
        }
        if (rateLimit.scope() != RateLimit.Scope.DEFAULT) {
            config.scope = rateLimit.scope().name().toLowerCase();
        }

        return config;
    }

    /**
     * 限流配置内部类
     */
    private static class RateLimitConfig {
        boolean enabled = true;
        int windowInSeconds;
        int maxRequests;
        String scope = "user";
    }

    /**
     * 获取限流操作
     */
    private RateLimit.Action getAction(RateLimit rateLimit) {
        if (rateLimit.action() != RateLimit.Action.DEFAULT) {
            return rateLimit.action();
        }

        // 使用全局配置
        String actionConfig = rateLimitProperties.getAction();
        return switch (actionConfig.toLowerCase()) {
            case "reject" -> RateLimit.Action.REJECT;
            case "queue" -> RateLimit.Action.QUEUE;
            case "delay" -> RateLimit.Action.DELAY;
            default -> RateLimit.Action.REJECT;
        };
    }

    /**
     * 处理超过限流的请求
     */
    private Object handleRateLimitExceeded(RateLimit.Action action, ProceedingJoinPoint joinPoint, RateLimit rateLimit)
            throws Throwable {
        if (action == RateLimit.Action.QUEUE) {
            log.debug("QUEUE 操作暂未实现，当前直接拒绝");
            throw new RateLimitException();
        } else if (action == RateLimit.Action.DELAY) {
            log.debug("DELAY 操作暂未实现，当前直接拒绝");
            throw new RateLimitException();
        } else {
            // REJECT 或默认行为
            throw new RateLimitException();
        }
    }

    /**
     * 从类所在的包名中提取模块名
     */
    private String extractModule(ProceedingJoinPoint joinPoint) {
        String packageName = joinPoint.getTarget().getClass().getPackage().getName();

        String basePackage = "com.evorsio.mybox.";
        if (packageName.startsWith(basePackage)) {
            String remaining = packageName.substring(basePackage.length());
            int dotIndex = remaining.indexOf('.');
            if (dotIndex > 0) {
                return remaining.substring(0, dotIndex);
            } else if (!remaining.isEmpty()) {
                return remaining;
            }
        }

        log.warn("无法从包名提取模块: {}", packageName);
        return "unknown";
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个 IP 的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * 从方法参数中提取用户 ID
     */
    private String extractUserId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CurrentUser.class)) {
                Object arg = args[i];
                if (arg instanceof UserPrincipal user) {
                    return user.getId().toString();
                } else {
                    log.warn("参数带有 @CurrentUser 注解，但类型不是 UserPrincipal: {}",
                            arg != null ? arg.getClass().getName() : "null");
                }
            }
        }

        return "anonymous";
    }
}
