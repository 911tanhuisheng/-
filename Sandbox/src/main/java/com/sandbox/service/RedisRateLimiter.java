package com.sandbox.service;

import com.sandbox.config.SandboxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
public class RedisRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final SandboxProperties sandboxProperties;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final Map<String, ConcurrentLinkedDeque<Long>> localWindows = new ConcurrentHashMap<>();

    public RedisRateLimiter(StringRedisTemplate stringRedisTemplate, SandboxProperties sandboxProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sandboxProperties = sandboxProperties;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setResultType(Long.class);
        this.rateLimitScript.setScriptText("""
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local member = ARGV[4]
                local expire = tonumber(ARGV[5])

                redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

                local current = redis.call('ZCARD', key)

                if current >= limit then
                    return 0
                end

                redis.call('ZADD', key, now, member)
                redis.call('EXPIRE', key, expire)

                return 1
                """);
    }

    public boolean allow(String key, long windowSeconds, long limit) {
        if (!sandboxProperties.getRateLimit().isEnabled()) {
            return true;
        }

        try {
            return allowByRedis(key, windowSeconds, limit);
        } catch (RedisConnectionFailureException e) {
            return allowByLocalFallback(key, windowSeconds, limit, e);
        } catch (Exception e) {
            return allowByLocalFallback(key, windowSeconds, limit, e);
        }
    }

    private boolean allowByRedis(String key, long windowSeconds, long limit) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000;
        String member = now + ":" + UUID.randomUUID();

        Long result = stringRedisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(windowMillis),
                String.valueOf(limit),
                member,
                String.valueOf(windowSeconds + 1)
        );

        return result != null && result == 1L;
    }

    private boolean allowByLocalFallback(String key, long windowSeconds, long limit, Exception cause) {
        if (!sandboxProperties.getRateLimit().isFallbackEnabled()) {
            log.warn("Redis rate limiter unavailable and local fallback disabled, key={}", key, cause);
            return false;
        }

        log.warn("Redis rate limiter unavailable, using local fallback, key={}", key, cause);
        shrinkLocalWindowsIfNeeded();

        long now = System.currentTimeMillis();
        long minAllowedTime = now - windowSeconds * 1000;
        ConcurrentLinkedDeque<Long> window = localWindows.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());

        synchronized (window) {
            while (!window.isEmpty()) {
                Long first = window.peekFirst();
                if (first == null || first >= minAllowedTime) {
                    break;
                }
                window.pollFirst();
            }

            if (window.size() >= limit) {
                return false;
            }

            window.addLast(now);
            return true;
        }
    }

    private void shrinkLocalWindowsIfNeeded() {
        int maxKeys = sandboxProperties.getRateLimit().getLocalMaxKeys();
        if (localWindows.size() <= maxKeys) {
            return;
        }

        Iterator<String> iterator = localWindows.keySet().iterator();
        int removeCount = Math.max(1, localWindows.size() - maxKeys);
        while (iterator.hasNext() && removeCount-- > 0) {
            iterator.next();
            iterator.remove();
        }
    }
}
