package com.evorsio.mybox.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Getter;

/**
 * 自定义用户主体（Principal）
 * <p>
 * 实现 Spring Security 的 UserDetails 接口，
 * 包含用户的完整信息，可以直接从 Authentication 中获取
 */
@Getter
@Builder
public class UserPrincipal implements UserDetails {
    
    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final UserRole role;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * 从 User 实体创建 UserPrincipal
     */
    public static UserPrincipal fromUser(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .build();
    }
}
