package com.spingbootinit.judo;

import com.spingbootinit.judo.strategy.DefaultJudgeStrategy;
import com.spingbootinit.judo.strategy.JavaLanguageJudgeStrategy;
import com.spingbootinit.judo.strategy.JudgeContext;
import com.spingbootinit.judo.strategy.JudgeStrategy;
import com.spingbootinit.judo.strategy.ObjectCountJudgeStrategy;
import com.spingbootinit.judo.strategy.ImageOutputJudgeStrategy;
import com.spingbootinit.model.dto.questionsubmit.JudgeInfo;
import com.spingbootinit.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;


/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return 判题结果
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if (judgeContext.getQuestion() != null && judgeContext.getQuestion().getQuestionType() != null
                && judgeContext.getQuestion().getQuestionType().toUpperCase().startsWith("IMAGE_")) {
            judgeStrategy = "IMAGE_OBJECT_COUNT".equalsIgnoreCase(judgeContext.getQuestion().getQuestionType())
                    ? new ObjectCountJudgeStrategy() : new ImageOutputJudgeStrategy();
        } else if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
