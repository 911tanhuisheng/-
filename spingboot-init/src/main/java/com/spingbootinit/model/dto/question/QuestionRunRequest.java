package com.spingbootinit.model.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 运行代码（样例用例，不入库、不判正式榜）
 * <p>题目 id 使用字符串，避免 JSON 数字超过 JS 安全整数时前端失真；后端再解析为 Long。</p>
 */
@Data
public class QuestionRunRequest implements Serializable {

    @NotBlank(message = "题目 id 不能为空")
    @Pattern(regexp = "\\d{1,24}", message = "题目 id 须为数字")
    private String questionId;

    @NotBlank(message = "语言不能为空")
    private String language;

    @NotBlank(message = "代码不能为空")
    @Size(max = 200_000, message = "代码过长")
    private String code;

    private static final long serialVersionUID = 1L;
}
