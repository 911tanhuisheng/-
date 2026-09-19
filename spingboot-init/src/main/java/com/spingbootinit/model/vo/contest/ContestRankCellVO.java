package com.spingbootinit.model.vo.contest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

@Data
public class ContestRankCellVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    private Integer score;

    private Long time;

    private Long memory;

    private String message;

    /** 本题 ICPC 罚时（分钟） */
    private Integer penaltyMinutes;

    /** 至首次 AC 的提交次数（未 AC 则为已完成提交数） */
    private Integer attempts;

    /** 已完成提交总数 */
    private Integer totalSubmissions;

    /**
     * 格内展示：AC 为「次数/本题罚时」如 2/65；未 AC 为「-3」表示 3 次未过
     */
    private String cellLabel;
}
