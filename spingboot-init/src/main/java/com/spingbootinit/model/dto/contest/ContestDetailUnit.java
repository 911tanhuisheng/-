package com.spingbootinit.model.dto.contest;

import lombok.Data;

import java.io.Serializable;

/**
 * 单题在竞赛榜中的统计（ICPC 风格：首次 AC、罚时、尝试次数）
 */
@Data
public class ContestDetailUnit implements Serializable {

    private Long questionId;

    /** 本题得分（AC 为满分，否则 0） */
    private Integer score;

    /** 首次 AC 那次提交的运行耗时 ms（仅 AC 有意义） */
    private Long time;

    /** 首次 AC 那次提交的内存（仅 AC 有意义） */
    private Long memory;

    private String message;

    /**
     * 本题 ICPC 罚时（分钟）：自开赛至首次 AC 的整分种数 + 20×首次 AC 前「非编译错误」的失败次数；未 AC 为 0
     */
    private Integer penaltyMinutes;

    /**
     * 首次 AC 前的有效提交次数（含首次 AC 那一次）；未 AC 则为已完成提交总数
     */
    private Integer attempts;

    /** 首次 AC 前计罚的错误提交次数（不含编译错误） */
    private Integer wrongBeforeAc;

    /** 本题已完成提交总数（含 AC 后提交） */
    private Integer totalSubmissions;
}
