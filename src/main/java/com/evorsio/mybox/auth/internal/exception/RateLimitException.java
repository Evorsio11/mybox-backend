package com.evorsio.mybox.auth.internal.exception;

/**
 * 速率限制异常
 * <p>
 * 当请求频率超过限制时抛出此异常
 */
public class RateLimitException extends AuthException {

    public RateLimitException() {
        super(com.evorsio.mybox.common.ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
