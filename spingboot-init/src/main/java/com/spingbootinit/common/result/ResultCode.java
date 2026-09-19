package com.spingbootinit.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "系统内部错误"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    /** 账号被禁用：已登录但不可使用业务功能 */
    ACCOUNT_DISABLED(40303, "账号已被禁用，暂不可使用此功能"),
    /** 禁用前签发的会话已作废，需重新登录 */
    ACCOUNT_SESSION_REVOKED(40304, "账号已被禁用，请重新登录"),
    /** 发布违禁评论，账号已被自动限制 */
    COMMENT_PROFANITY_BANNED(40305, "发布违禁评论，评论功能已被限制"),
    /** 其他设备登录导致当前会话失效 */
    LOGIN_ELSEWHERE(40306, "您的账号已在其他设备登录，请重新登录"),
    /** AI 学习助手发送违禁内容，助手功能已被限制 */
    AI_ASSIST_PROFANITY_BANNED(40307, "向 AI 助手发送违禁内容，学习助手已被限制"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败");

    private final int code;
    private final String message;
}