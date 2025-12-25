package com.evorsio.mybox.auth.filter;

import com.evorsio.mybox.auth.util.JwtUtil;
import com.evorsio.mybox.common.properties.AuthJwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
                    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
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
