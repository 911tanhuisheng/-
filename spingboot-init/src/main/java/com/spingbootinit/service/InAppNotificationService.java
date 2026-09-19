package com.spingbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spingbootinit.model.vo.notification.InAppNotificationItemVO;
import com.spingbootinit.model.vo.notification.NotificationSummaryVO;

import java.util.List;

public interface InAppNotificationService {

    NotificationSummaryVO summary(long userId);

    Page<InAppNotificationItemVO> pageMine(long userId, long current, long pageSize);

    void markRead(long userId, List<Long> ids);

    void markAllRead(long userId);

    /**
     * 插入一条通知；bizKey 非空时同一用户 + bizKey 仅一条（用于开赛提醒去重）
     */
    void send(long recipientUserId, int type, String title, String body, String linkKind, String linkRef, String bizKey);
}
