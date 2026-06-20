package com.example.chunktranslate.security;

import com.example.chunktranslate.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌工具类，负责 access token 的签发、验证、解析。
 * <p>refresh token 使用 UUID 生成并存储在 refresh_token 表中，不嵌入 JWT。
 * 签名密钥从 application.yml 的 jwt.secret 读取，使用 HMAC-SHA 算法。</p>
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshExpiration;

    /**
     * 签发 access token，payload 包含 userId(subject) 和 role(claim)
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 签发 refresh token（UUID，不嵌入 JWT，存储在 DB 中）
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * 验证并解析 JWT，返回 Claims；token 无效时抛出 JwtException
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 中提取 userId
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(validateToken(token).getSubject());
    }

    /**
     * 从 token 中提取 role
     */
    public String getRoleFromToken(String token) {
        return validateToken(token).get("role", String.class);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
