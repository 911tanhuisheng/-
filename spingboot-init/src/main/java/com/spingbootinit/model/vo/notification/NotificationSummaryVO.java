package com.spingbootinit.model.vo.notification;

import lombok.Data;

@Data
public class NotificationSummaryVO {

    /** 站内未读条数 */
    private long inAppUnread;

    /** 未读公告条数（比用户已读游标新的已发布公告） */
    private long announcementUnread;
}
