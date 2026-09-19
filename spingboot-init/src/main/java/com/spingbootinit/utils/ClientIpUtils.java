package com.spingbootinit.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从 HTTP 请求解析客户端真实 IP（直连或经反向代理 / CDN）。
 * <p>
 * 优先读取常见代理头（X-Forwarded-For、X-Real-IP 等），再回退到 {@link HttpServletRequest#getRemoteAddr()}。
 * 生产环境建议在网关/反向代理上覆盖不可信客户端伪造的 X-Forwarded-For。
 */
public final class ClientIpUtils {

    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP",
    };

    private ClientIpUtils() {
    }

    /**
     * 解析客户端真实 IP（直连或经反向代理 / CDN）。
     * <p>
     * 优先读取常见代理头（X-Forwarded-For、X-Real-IP 等），再回退到 {@link HttpServletRequest#getRemoteAddr()}。
     * 生产环境建议在网关/反向代理上覆盖不可信客户端伪造的 X-Forwarded-For。
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = null;
        for (String name : PROXY_HEADERS) {
            String v = request.getHeader(name);
            if (v == null) {
                continue;
            }
            v = v.trim();
            if (v.isEmpty() || "unknown".equalsIgnoreCase(v)) {
                continue;
            }
            ip = v;
            break;
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能为：client, proxy1, proxy2 —— 取最左侧（原始客户端）
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
