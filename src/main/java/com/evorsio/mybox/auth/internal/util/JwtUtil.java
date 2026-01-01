package com.evorsio.mybox.auth.internal.util;

import com.evorsio.mybox.auth.TokenType;
import com.evorsio.mybox.auth.internal.properties.AuthJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final AuthJwtProperties authJwtProperties;

    public String generateToken(String subject, Map<String, Object> claims, TokenType tokenType) {
        Date now = new Date();
        long expiration = tokenType == TokenType.ACCESS
                ? authJwtProperties.getExpiration()
                : authJwtProperties.getRefreshExpiration();

        Date exp = new Date(now.getTime() + expiration * 1000);

        Map<String, Object> tokenClaims = new HashMap<>();

        if (tokenType == TokenType.ACCESS && claims != null) {
            tokenClaims.putAll(claims);
        }

        JwtBuilder builder = Jwts.builder()
                .claim("type", tokenType.name())
                .claims(tokenClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .issuer(authJwtProperties.getIssuer())
                .audience().add(authJwtProperties.getAudience()).and()
                .signWith(signingKey(), Jwts.SIG.HS256);

        return builder.compact();
    }

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, String expectedIssuer) {
        try {
            Claims claims = parseToken(token);
            String issuer = claims.getIssuer();
            if (!expectedIssuer.equals(issuer)) {
                return false;
            }
            if (!claims.getAudience().contains(authJwtProperties.getAudience())) {
                return false;
            }
            Date expiration = claims.getExpiration();
            return !expiration.before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(authJwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
