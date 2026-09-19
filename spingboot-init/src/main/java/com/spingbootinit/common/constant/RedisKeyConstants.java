package com.spingbootinit.common.constant;

/**
 * Redis Key 统一管理。
 * <p>
 * 约定：业务前缀:模块:含义:标识
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }
    public static final String CAPTCHA_LIMIT_PREFIX = "captcha:limit:";
    public static final String CAPTCHA_CODE_PREFIX = "captcha:code:";
    public static String captchaLimitKey(String clientIp) {
        return CAPTCHA_LIMIT_PREFIX + clientIp;
    }
    public static String captchaCodeKey(String clientIp) {
        return CAPTCHA_CODE_PREFIX + clientIp;
    }

    /** refresh token 白名单：auth:refresh:{userId}:{jti} */
    public static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    public static String refreshTokenKey(Long userId, String jti) {
        return REFRESH_TOKEN_PREFIX + userId + ":" + jti;
    }

    /** 管理员禁用用户时记录时间戳（毫秒），用于作废禁用前签发的 token */
    public static final String DISABLED_AT_PREFIX = "auth:disabled-at:";

    public static String disabledAtKey(Long userId) {
        return DISABLED_AT_PREFIX + userId;
    }

    public static String refreshTokenPattern(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId + ":";
    }

    /** 当前唯一登录会话 id（同账号新设备登录会轮换，旧 token 失效） */
    public static final String ACTIVE_SESSION_PREFIX = "auth:active-session:";

    public static String activeSessionKey(Long userId) {
        return ACTIVE_SESSION_PREFIX + userId;
    }

    /** 账号状态版本号（管理员禁用/解禁、评论限制等变更时递增，供 SSE/轮询感知） */
    public static final String STATUS_REVISION_PREFIX = "auth:status-rev:";

    public static String statusRevisionKey(Long userId) {
        return STATUS_REVISION_PREFIX + userId;
    }

}
