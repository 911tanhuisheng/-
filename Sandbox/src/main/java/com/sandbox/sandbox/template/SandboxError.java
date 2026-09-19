package com.sandbox.sandbox.template;

/**
 * 沙箱错误类型。
 * code 用于调用方稳定判断，status 保留兼容旧 OJ 状态，resultType 用于语义化识别结果。
 */
public enum SandboxError {
    SUCCESS(0, 2, "SUCCESS"),
    BAD_REQUEST(1001, 4, "BAD_REQUEST"),
    COMPILE_ERROR(1002, 3, "COMPILE_ERROR"),
    RUNTIME_ERROR(1003, 2, "RUNTIME_ERROR"),
    TIME_LIMIT(1004, 2, "TIME_LIMIT"),
    SECURITY_REJECT(1005, 4, "SECURITY_REJECT"),
    SYSTEM_ERROR(1006, 4, "SYSTEM_ERROR"),
    RATE_LIMITED(1007, 4, "RATE_LIMITED"),
    SERVER_BUSY(1008, 4, "SERVER_BUSY");

    public final int code;
    public final int status;
    public final String resultType;

    SandboxError(int code, int status, String resultType) {
        this.code = code;
        this.status = status;
        this.resultType = resultType;
    }
}
