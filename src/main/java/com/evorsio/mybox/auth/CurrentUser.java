package com.evorsio.mybox.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注入当前登录用户信息的注解
 * <p>
 * 用法示例：
 * <pre>
 * public ResponseEntity<?> someMethod(@CurrentUser UserPrincipal user) {
 *     UUID userId = user.getId();
 *     String username = user.getUsername();
 *     UserRole role = user.getRole();
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
