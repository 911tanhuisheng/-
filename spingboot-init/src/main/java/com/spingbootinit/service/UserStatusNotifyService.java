package com.spingbootinit.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户账号状态变更通知（SSE 推送 + Redis 版本号，供前端秒级同步）。
 */
public interface UserStatusNotifyService {

    /** 状态变更后递增版本并推送给在线 SSE 连接 */
    long bumpStatusRevision(Long userId);

    Long getStatusRevision(Long userId);

    SseEmitter subscribe(Long userId);
}
