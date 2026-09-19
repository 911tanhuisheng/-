package com.spingbootinit.model.vo.bailian;

import com.spingbootinit.model.dto.question.JudgeCase;
import com.spingbootinit.model.dto.question.JudgeConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProblemStructureVO {
    private String title;
    private String content;
    private String answer;
    private String difficulty;
    private List<String> tags = new ArrayList<>();
    private List<JudgeCase> judgeCase = new ArrayList<>();
    private JudgeConfig judgeConfig;
    private String rawOcrText;
}
