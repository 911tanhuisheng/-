package com.sandbox.aop;

import com.sandbox.annotation.RateLimit;
import com.sandbox.config.SandboxProperties;
import com.sandbox.sandbox.template.SandboxError;
import com.sandbox.sandbox.template.SandboxException;
import com.sandbox.service.RedisRateLimiter;
import com.sandbox.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RateLimitAspect {

    private final RedisRateLimiter redisRateLimiter;
    private final SandboxProperties sandboxProperties;
    private final JwtUtils jwtUtils;

    public RateLimitAspect(RedisRateLimiter redisRateLimiter, SandboxProperties sandboxProperties, JwtUtils jwtUtils) {
        this.redisRateLimiter = redisRateLimiter;
        this.sandboxProperties = sandboxProperties;
        this.jwtUtils = jwtUtils;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getRequest();
        String principalKey = resolvePrincipalKey(request);
        String redisKey = buildRedisKey(rateLimit.key(), principalKey);
        long windowSeconds = sandboxProperties.getRateLimit().getWindowSeconds() > 0
                ? sandboxProperties.getRateLimit().getWindowSeconds()
                : rateLimit.windowSeconds();
        long limit = sandboxProperties.getRateLimit().getLimit() > 0
                ? sandboxProperties.getRateLimit().getLimit()
                : rateLimit.limit();

        boolean allowed = redisRateLimiter.allow(redisKey, windowSeconds, limit);
        if (!allowed) {
            throw new SandboxException(
                    SandboxError.RATE_LIMITED,
                    "请求过于频繁，请稍后再试",
                    "rate limit exceeded"
            );
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new SandboxException(
                    SandboxError.SYSTEM_ERROR,
                    "系统内部异常",
                    "request attributes is null"
            );
        }
        return attributes.getRequest();
    }

    private String buildRedisKey(String businessKey, String principalKey) {
        return "rate_limit:" + businessKey + ":" + principalKey;
    }

    /**
     * 限流优先按 JWT 里的 userId，无法验证身份时回退到 IP。
     * 注意：不信任客户端透传的 X-User-Id，避免伪造 header 绕过限流。
     */
    private String resolvePrincipalKey(HttpServletRequest request) {
        String auth = trimToNull(request.getHeader("Authorization"));
        if (auth != null) {
            String bearerPrefix = "Bearer ";
            String token = auth.startsWith(bearerPrefix) ? auth.substring(bearerPrefix.length()).trim() : auth.trim();
            if (!token.isEmpty() && jwtUtils.validateToken(token)) {
                Long uid = jwtUtils.getUserIdFromToken(token);
                if (uid != null) {
                    return "uid:" + uid;
                }
            }
        }

        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
