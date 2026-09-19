package com.spingbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.Result;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.dto.notification.AnnouncementReadUpToRequest;
import com.spingbootinit.model.dto.notification.NotificationMarkReadRequest;
import com.spingbootinit.model.vo.notification.InAppNotificationItemVO;
import com.spingbootinit.model.vo.notification.NotificationSummaryVO;
import com.spingbootinit.service.InAppNotificationService;
import com.spingbootinit.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/notification")
@Tag(name = "站内通知", description = "评论、比赛、公告相关提醒")
public class NotificationController {

    @Resource
    private InAppNotificationService inAppNotificationService;

    @Resource
    private UserService userService;

    @GetMapping("/summary")
    @Operation(summary = "未读汇总")
    public Result<NotificationSummaryVO> summary() {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(inAppNotificationService.summary(uid));
    }

    @GetMapping("/page")
    @Operation(summary = "我的站内通知分页")
    public Result<Page<InAppNotificationItemVO>> page(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(inAppNotificationService.pageMine(uid, current, pageSize));
    }

    @PostMapping("/mark-read")
    @Operation(summary = "标记已读")
    public Result<Void> markRead(@Valid @RequestBody NotificationMarkReadRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        List<Long> ids = new ArrayList<>();
        for (String s : request.getIds()) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        inAppNotificationService.markRead(uid, ids);
        return Result.success(null);
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllRead() {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        inAppNotificationService.markAllRead(uid);
        return Result.success(null);
    }

    @PostMapping("/announcement/read-up-to")
    @Operation(summary = "公告已读到指定 id（游标）")
    public Result<Void> readAnnouncementUpTo(@Valid @RequestBody AnnouncementReadUpToRequest body) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        long aid;
        try {
            aid = Long.parseLong(body.getId().trim());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "公告 id 无效");
        }
        userService.updateLastReadAnnouncementUpTo(uid, aid);
        return Result.success(null);
    }
}
