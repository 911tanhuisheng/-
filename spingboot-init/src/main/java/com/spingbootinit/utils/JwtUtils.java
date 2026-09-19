package com.spingbootinit.utils;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

    public static final String CLAIM_TYP = "typ";
    public static final String TYP_ACCESS = "access";
    public static final String TYP_REFRESH = "refresh";
    public static final String CLAIM_JTI = "jti";
    /** 登录会话 id，同账号仅保留最新一次登录 */
    public static final String CLAIM_SID = "sid";

    @Value("${jwt.secret}")
    private String secret;

    /** 兼容旧配置：未单独配置 access 时回退到此值（秒） */
    @Value("${jwt.expiration:86400}")
    private Long expirationSeconds;

    @Value("${jwt.access-expiration:${jwt.expiration:86400}}")
    private Long accessExpirationSeconds;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpirationSeconds;

    private SecretKey signingKey;

    @Getter
    private long accessTtlSeconds;

    @Getter
    private long refreshTtlSeconds;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密钥不能为空");
        }
        accessTtlSeconds = accessExpirationSeconds != null && accessExpirationSeconds > 0
                ? accessExpirationSeconds
                : (expirationSeconds != null && expirationSeconds > 0 ? expirationSeconds : 86400L);
        refreshTtlSeconds = refreshExpirationSeconds != null && refreshExpirationSeconds > 0
                ? refreshExpirationSeconds
                : 604800L;
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT 密钥长度不足 32 个字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

  /**
   * 签发短期 access token（API 鉴权用）。
   */
    public String generateAccessToken(Long userId, String username, String sessionId) {
        return buildToken(userId, username, TYP_ACCESS, null, sessionId, accessTtlSeconds);
    }

    /**
     * 签发长期 refresh token，并返回其中的 jti（用于 Redis 白名单 / 轮换）。
     */
    public IssuedRefreshToken generateRefreshToken(Long userId, String username, String sessionId) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String token = buildToken(userId, username, TYP_REFRESH, jti, sessionId, refreshTtlSeconds);
        return new IssuedRefreshToken(token, jti);
    }

    public String getSessionIdFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(CLAIM_SID, String.class);
        } catch (Exception e) {
            log.warn("从 JWT 获取 sid 失败: {}", e.getMessage());
            return null;
        }
    }

    private String buildToken(Long userId, String username, String typ, String jti, String sessionId, long ttlSeconds) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (username == null) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlSeconds * 1000);
        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim(CLAIM_TYP, typ)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256);
        if (jti != null && !jti.isEmpty()) {
            builder.claim(CLAIM_JTI, jti);
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            builder.claim(CLAIM_SID, sessionId);
        }
        return builder.compact();
    }

    public boolean validateAccessToken(String token) {
        return validateTokenOfType(token, TYP_ACCESS, true);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenOfType(token, TYP_REFRESH, false);
    }

    /**
     * @param allowLegacyWithoutTyp 无 typ 声明的旧 token 视为 access（平滑升级）
     */
    private boolean validateTokenOfType(String token, String expectedTyp, boolean allowLegacyWithoutTyp) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Claims claims = parseClaims(token);
            String typ = claims.get(CLAIM_TYP, String.class);
            if (typ == null || typ.isEmpty()) {
                return allowLegacyWithoutTyp && TYP_ACCESS.equals(expectedTyp);
            }
            return expectedTyp.equals(typ);
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

    /** @deprecated 请使用 {@link #validateAccessToken(String)} */
    public boolean validateToken(String token) {
        return validateAccessToken(token);
    }

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

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.warn("从 JWT 获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    public String getJtiFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(CLAIM_JTI, String.class);
        } catch (Exception e) {
            log.warn("从 JWT 获取 jti 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 token 签发时间（毫秒），用于判断禁用前签发的会话是否应作废。
     */
    public Long getIssuedAtMillisFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            Date iat = claims.getIssuedAt();
            return iat == null ? null : iat.getTime();
        } catch (Exception e) {
            log.warn("从 JWT 获取签发时间失败: {}", e.getMessage());
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public record IssuedRefreshToken(String token, String jti) {}
}
