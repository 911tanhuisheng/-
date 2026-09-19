package com.spingbootinit.model.dto.question;

import lombok.Data;

import java.util.List;

@Data
public class QuestionQueryRequest extends PageRequest{

    /**
     * 题目 id（精确匹配）
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /** 题型：TEXT 或 IMAGE_* 图像编程题 */
    private String questionType;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 出题人昵称（模糊匹配）
     */
    private String userNickname;

    private static final long serialVersionUID = 1L;

}
