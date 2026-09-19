package com.spingbootinit.model.vo.contest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ContestAdminListItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private Date startTime;

    private Date endTime;

    /**
     * 未开始 / 进行中 / 已结束
     */
    private String phase;

    private Integer questionCount;
}
