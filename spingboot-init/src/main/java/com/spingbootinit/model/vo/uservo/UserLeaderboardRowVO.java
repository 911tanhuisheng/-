package com.spingbootinit.model.vo.uservo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

/**
 * 全站用户榜单行（做题 AC 数 + 积分）。
 * 仅包含已在至少一场竞赛中报名（contest_user）的用户。
 */
@Data
public class UserLeaderboardRowVO implements Serializable {

    /** 当前页内名次（跨页连续编号） */
    private Integer rankOrder;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String displayName;

    private String avatar;

    /** 至少有一次 AC 的题目数（status=成功 且判题为 Accepted） */
    private Integer solvedCount;

    private Integer points;

    /** 总提交次数（含未 AC） */
    private Integer submitCount;
}
