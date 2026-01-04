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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(authJwtProperties.getTokenPrefix())) {
            String token = authHeader.substring(authJwtProperties.getTokenPrefix().length()).trim();  // 去掉空格

            try {
                log.info("正在处理 token: {}", token);

                // 验证 token 是否有效
                if (!jwtUtil.validateToken(token, authJwtProperties.getIssuer())) {
                    log.warn("无效或过期的令牌: {}", token);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "无效或者过期的令牌");
                    return;
                }

                // 解析 token 获取用户名等信息
                Claims claims = jwtUtil.parseToken(token);
                String username = claims.get("username", String.class);

                if (username != null) {
                    log.info("认证成功，用户名: {}", username);

                    // 加载用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 从 JWT 的 subject 中获取用户 ID (UUID)
                    String sub = claims.getSubject(); // JWT sub 应该是用户 ID
                    UUID userId = UUID.fromString(sub);

                    // 将 userId 作为 principal，而不是 UserDetails
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, userDetails.getAuthorities());

                    // 可以将 UserDetails 放到 details 中，如果后续需要的话
                    authentication.setDetails(userDetails);

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
