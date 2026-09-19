package com.spingbootinit.model.vo.uservo;

import lombok.Data;

/**
 * 刷新令牌接口返回：新的 access + refresh（轮换 refresh，旧 refresh 作废）。
 */
@Data
public class TokenRefreshVO {
    /** 短期访问令牌，请求 API 时放在 Authorization: Bearer */
    private String accessToken;
    /** 长期刷新令牌，仅用于 /user/refresh */
    private String refreshToken;
    /**
     * 与 accessToken 相同，兼容仍读取 {@code token} 字段的旧前端。
     */
    private String token;
}
