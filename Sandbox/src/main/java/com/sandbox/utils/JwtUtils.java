package com.sandbox.utils;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expirationSeconds; // 单位：秒


    // 创建的秘钥
    private SecretKey signingKey;

    /**
     * 启动时校验秘钥并初始化 SecretKey
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密钥不能为空");
        }
        if (expirationSeconds == null || expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT 过期时间必须为正整数（单位：秒）");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT 密钥长度不足 32 个字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);

    }


    /**
     * 生成jwt token
     */
    public String generateToken(Long userId, String username) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (username == null) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        Date now = new Date(); // 获取当前时间
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000); // 秒 -> 毫秒
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 通常为用户的唯一标识符
                .claim("username", username) // 添加用户名 添加自定义声明
                .setIssuedAt(now) // 设置签发时间
                .setExpiration(expiryDate) // 设置过期时间
                .signWith(signingKey, SignatureAlgorithm.HS256) // 使用密钥和签名算法进行签名
                .compact(); // 生成token
    }

    /**
     * 验证 Token 是否有效（未过期、签名正确、格式合法）
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT token 格式: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token 格式错误: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token 参数非法: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT token 解析异常", e);
        }
        return false;
    }

    /**
     * 从 Token 中安全获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String subject = claims.getSubject();
            if (subject == null || subject.isEmpty()) {
                log.warn("JWT 中缺少 subject 字段");
                return null;
            }
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            log.warn("JWT subject 不是有效的用户ID: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("从 JWT 获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.warn("从 JWT 获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 内部方法：解析 Token 并返回 Claims（假设已通过 validateToken 验证）
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}