package com.spingbootinit.model.vo.uservo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 当前登录用户的账号状态（供前端轮询：禁用/解禁实时生效）。
 */
@Data
public class UserSessionStatusVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    /** 0-禁用 1-正常 */
    private Integer status;
    /** 封禁类型：admin / profanity / profanity_ai */
    private String banType;
    private String banReason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date banUntil;
    /** 是否因违禁评论被限制发表评论（status 可能仍为 1） */
    private Boolean commentBanned;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date commentBanUntil;
    private String commentBanReason;
    /** 是否因向 AI 助手发送违禁内容被限制使用学习助手（status 可能仍为 1） */
    private Boolean aiAssistBanned;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date aiBanUntil;
    private String aiBanReason;
    /** 状态版本号（变更时递增，前端对比后决定是否刷新 UI） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long statusRevision;
}
