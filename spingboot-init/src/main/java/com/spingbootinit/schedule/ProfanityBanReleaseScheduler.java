package com.spingbootinit.schedule;

import com.spingbootinit.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时解禁已到期的封禁（违禁评论限制、管理员限时禁用）。
 */
@Component
@Slf4j
public class ProfanityBanReleaseScheduler {

    @Resource
    private UserService userService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void releaseExpiredBans() {
        try {
            userService.releaseExpiredProfanityBans();
        } catch (Exception e) {
            log.error("解禁到期违禁评论限制失败", e);
        }
        try {
            userService.releaseExpiredProfanityAiBans();
        } catch (Exception e) {
            log.error("解禁到期 AI 助手违禁限制失败", e);
        }
        try {
            userService.releaseExpiredAdminBans();
        } catch (Exception e) {
            log.error("解禁到期管理员限时禁用失败", e);
        }
    }
}
