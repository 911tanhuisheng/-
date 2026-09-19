package com.spingbootinit.model.dto.question;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionBatchDeleteRequest {
    @NotEmpty(message = "待删除题目 id 列表不能为空")
    private List<@NotNull(message = "题目 id 不能为空") Long> ids;
}
