package com.spingbootinit.model.vo.questionvo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.spingbootinit.model.dto.question.JudgeCase;
import com.spingbootinit.model.dto.question.JudgeConfig;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class QuestionPublicDetailVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String content;
    private String questionType;
    private String imageUrl;
    private String visionModelKey;
    private Integer countTolerance;
    private List<String> tags;
    private Integer submitNum;
    private Integer acceptedNum;
    private JudgeConfig judgeConfig;
    /**
     * 公开展示的样例用例（只展示前几条）
     */
    private List<JudgeCase> sampleJudgeCase;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String userNickname;
    private Date createTime;
    private Date updateTime;
}
