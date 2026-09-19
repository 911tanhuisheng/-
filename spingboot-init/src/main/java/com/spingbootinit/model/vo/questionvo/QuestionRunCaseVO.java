package com.spingbootinit.model.vo.questionvo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次「运行」中单个样例的输入输出与是否通过（仅比对展示，与正式提交评测独立）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRunCaseVO {

    private int index;

    private String input;

    private String expectedOutput;

    private String actualOutput;

    /** 与预期在规范化后是否一致 */
    private boolean passed;
}
