package com.evorsio.mybox.auth.util;

import com.evorsio.mybox.auth.domain.User;

import java.util.HashMap;
import java.util.Map;

public class JwtClaimsBuilder {
    public static Map<String ,Object> build(User user){
        Map<String,Object> claims = new HashMap<>();
        claims.put("username",user.getUsername());
        claims.put("email",user.getEmail());
        claims.put("role",user.getRole().name());
        return claims;
    }
}
