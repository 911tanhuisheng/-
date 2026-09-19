package com.spingbootinit.model.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 题目用例
 */
@Data
public class JudgeCase {

    /**
     * 输入用例
     */
    @NotBlank(message = "输入用例不能为空")
    @Size(max = 20000, message = "输入用例不能超过20000个字符")
    private String input;

    /**
     * 输出用例
     */
    @NotBlank(message = "输出用例不能为空")
    @Size(max = 20000, message = "输出用例不能超过20000个字符")
    private String output;

    /** 是否为公开样例；false 为仅在正式提交时使用的隐藏用例。 */
    private Boolean sample = false;
}
