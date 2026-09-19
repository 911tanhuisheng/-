package com.spingbootinit.model.dto.questionsubmit;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 */
@Data
public class QuestionSubmitAddRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 可选：竞赛 id；传入时须已报名且题目属于该赛、在时间窗内
     */
    private Long contestId;

    private static final long serialVersionUID = 1L;
}