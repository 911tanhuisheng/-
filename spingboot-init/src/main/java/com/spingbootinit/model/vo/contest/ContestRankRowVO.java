package com.spingbootinit.model.vo.contest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ContestRankRowVO implements Serializable {

    private Integer rankOrder;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userName;

    /** 通过题数（与满分一致的 AC 题数） */
    private Integer solvedCount;

    /** 总罚时（分钟），各题 penaltyMinutes 之和 */
    private Integer totalPenalty;

    /** 总分（OI 部分分场景；ICPC 下与通过题数一致时可作参考） */
    private Integer totalScore;

    /** 各题首次 AC 的运行耗时之和 ms */
    private Long totalTime;

    /** 各题首次 AC 的内存之和 */
    private Long totalMemory;

    private List<ContestRankCellVO> cells;
}
