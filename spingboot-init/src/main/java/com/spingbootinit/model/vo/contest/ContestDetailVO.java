package com.spingbootinit.model.vo.contest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ContestDetailVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String description;

    private Date startTime;

    private Date endTime;

    private String phase;

    /**
     * 当前登录用户是否已报名；未登录时为 null。
     */
    private Boolean meRegistered;

    private List<ContestQuestionBriefVO> questions;
}
