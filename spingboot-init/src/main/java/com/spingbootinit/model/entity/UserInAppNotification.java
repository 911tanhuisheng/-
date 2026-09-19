package com.spingbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 站内个人通知（评论、比赛相关等）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("user_inapp_notification")
public class UserInAppNotification extends BaseEntity {

    public static final int TYPE_COMMENT_ON_POST = 1;
    public static final int TYPE_COMMENT_REPLY = 2;
    public static final int TYPE_CONTEST_JOIN = 3;
    public static final int TYPE_CONTEST_SOON = 4;
    /** 违禁评论封禁通知 */
    public static final int TYPE_ACCOUNT_PROFANITY_BAN = 5;
    /** 违禁封禁期满自动解禁 */
    public static final int TYPE_ACCOUNT_PROFANITY_UNBAN = 6;
    /** 管理员限时禁用 */
    public static final int TYPE_ACCOUNT_ADMIN_BAN = 7;
    /** 管理员限时禁用期满自动解禁 */
    public static final int TYPE_ACCOUNT_ADMIN_UNBAN = 8;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private Integer type;

    private String title;

    private String body;

    /** BLOG_POST / CONTEST */
    private String linkKind;

    private String linkRef;

    private String bizKey;

    private Date readAt;
}
