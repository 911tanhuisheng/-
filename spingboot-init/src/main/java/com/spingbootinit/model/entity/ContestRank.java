package com.spingbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

@TableName("contest_rank")
@Data
public class ContestRank extends BaseEntity implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long contestId;

    private Long userId;

    private String userName;

    private Integer totalScore;

    private Long totalTime;

    private Long totalMemory;

    /**
     * JSON：questionId 字符串 -> 单题最优单元
     */
    private String gameDetail;
}
