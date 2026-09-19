package com.spingbootinit.service;

import com.spingbootinit.model.dto.question.QuestionRunRequest;
import com.spingbootinit.model.vo.questionvo.QuestionRunVO;

/**
 * 题目「运行」：仅样例用例 + 沙箱，不写提交记录
 */
public interface QuestionRunService {

    QuestionRunVO runSample(QuestionRunRequest request);
}
