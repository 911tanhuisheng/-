package com.spingbootinit.model.vo.contest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

@Data
public class ContestQuestionBriefVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    private String questionTitle;

    private Integer fullScore;

    private Integer sortOrder;
}
