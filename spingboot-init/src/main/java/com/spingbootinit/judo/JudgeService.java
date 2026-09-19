package com.spingbootinit.judo;

import com.spingbootinit.model.entity.QuestionSubmit;


public interface JudgeService {
    /**
     * 判题的接口
     * @param questionSubmitId 题目提交 id
     * @return 判题结果
     */
    QuestionSubmit doJudge(long questionSubmitId);
}
