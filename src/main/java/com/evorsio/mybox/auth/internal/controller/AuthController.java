package com.evorsio.mybox.auth.internal.controller;

import com.evorsio.mybox.auth.*;
import com.evorsio.mybox.auth.internal.exception.AuthException;
import com.evorsio.mybox.auth.internal.util.JwtUtil;
import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     * <p>
     * 限流策略：按 IP 地址限流，5分钟内最多 5 次登录尝试
     * 防止暴力破解攻击
     */
    @PostMapping("/login")
    @RateLimit(scope = RateLimit.Scope.IP, window = "5m", maxRequests = 5)
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse token = authService.login(
                request.getUsername(),
                request.getPassword(),
                request.getDeviceInfo()
        );
        return ApiResponse.success("登录成功", token);
    }

    /**
     * 用户注册
     * <p>
     * 限流策略：按 IP 地址限流，1小时内最多 3 次注册尝试
     * 防止恶意批量注册
     */
    @PostMapping("/register")
    @RateLimit(scope = RateLimit.Scope.IP, window = "1h", maxRequests = 3)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getDeviceInfo()
        );
        return ApiResponse.success();
    }

    /**
     * 刷新访问令牌
     * <p>
     * 限流策略：按用户限流，1分钟内最多 10 次刷新
     * 防止令牌刷新滥用
     */
    @PostMapping("/refresh")
    @RateLimit(scope = RateLimit.Scope.USER, window = "1m", maxRequests = 10)
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        UUID userId = UUID.fromString(jwtUtil.parseToken(refreshToken).getSubject());
        TokenResponse token = authService.refreshToken(userId, refreshToken);
        return ApiResponse.success("刷新令牌成功", token);
    }

    /**
     * 用户登出
     * <p>
     * 限流策略：按用户限流，1分钟内最多 10 次登出
     * 从 Authorization 头中提取 AccessToken，删除 RefreshToken 并将 AccessToken 加入黑名单
     *
     * @param user          当前登录用户
     * @param request       HTTP 请求（用于提取 Authorization 头）
     * @return 成功响应
     */
    @PostMapping("/logout")
    @RateLimit(scope = RateLimit.Scope.USER, window = "1m", maxRequests = 10)
    public ApiResponse<Void> logout(@CurrentUser UserPrincipal user, HttpServletRequest request) {
        UUID userId = user.getId();

        // 从 Authorization 头中提取 AccessToken
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(jwtUtil.getAuthJwtProperties().getTokenPrefix())) {
            throw new AuthException(ErrorCode.MISSING_AUTH_HEADER);
        }

        String accessToken = authHeader.substring(jwtUtil.getAuthJwtProperties().getTokenPrefix().length()).trim();

        // 执行登出：删除 RefreshToken 并将 AccessToken 加入黑名单
        authService.logout(userId, accessToken);

        return ApiResponse.success("登出成功，访问令牌已撤销");
    }
}
