package com.evorsio.mybox.auth.internal.filter;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.evorsio.mybox.auth.TokenBlacklistService;
import com.evorsio.mybox.auth.internal.properties.AuthJwtProperties;
import com.evorsio.mybox.auth.internal.util.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final AuthJwtProperties authJwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(authJwtProperties.getTokenPrefix())) {
            String token = authHeader.substring(authJwtProperties.getTokenPrefix().length()).trim();  // 去掉空格

            try {
                log.debug("正在验证 token: {}", token.substring(0, Math.min(20, token.length())) + "...");

                // 解析 token 获取用户名等信息
                Claims claims = jwtUtil.parseToken(token);
                String userIdStr = claims.getSubject();
                String username = claims.get("username", String.class);

                // 验证 token 是否有效
                if (!jwtUtil.validateToken(token, authJwtProperties.getIssuer())) {
                    log.warn("无效或过期的令牌: userId={}", userIdStr);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "无效或者过期的令牌");
                    return;
                }

                // 检查 token 是否在黑名单中
                UUID userId = UUID.fromString(userIdStr);
                if (tokenBlacklistService.isBlacklisted(userId, token)) {
                    log.warn("Token 已被撤销（在黑名单中）: userId={}", userId);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "令牌已被撤销");
                    return;
                }

                if (username != null) {
                    log.debug("认证成功，用户名: {}, userId: {}", username, userId);

                    // 加载用户信息（返回 UserPrincipal）
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 将 UserPrincipal 作为 principal（包含 ID、用户名、角色等完整信息）
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,  // UserPrincipal
                                    null,
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (JwtException | IllegalArgumentException e) {
                log.error("令牌验证失败: {}", e.getMessage(), e);
                // 捕获异常并返回 Unauthorized 错误
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "无效的令牌");
                return;
            }
        }

        filterChain.doFilter(request, response);  // 继续过滤链
    }
}
