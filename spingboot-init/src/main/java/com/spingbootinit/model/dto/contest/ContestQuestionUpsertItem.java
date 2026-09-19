package com.spingbootinit.model.dto.contest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class ContestQuestionUpsertItem implements Serializable {

    /**
     * 雪花 id 超出 JS 安全整数，前端须以字符串传递；服务端再解析为 Long。
     */
    @NotBlank(message = "题目 id 不能为空")
    private String questionId;

    /**
     * 满分，默认 100
     */
    @Min(value = 1, message = "满分至少为 1")
    private Integer fullScore;

    /**
     * 展示顺序，越小越靠前；可不传，按列表顺序自动生成
     */
    private Integer sortOrder;
}
