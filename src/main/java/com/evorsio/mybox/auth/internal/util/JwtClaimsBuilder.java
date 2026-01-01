package com.evorsio.mybox.auth.internal.util;

import com.evorsio.mybox.auth.User;

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
