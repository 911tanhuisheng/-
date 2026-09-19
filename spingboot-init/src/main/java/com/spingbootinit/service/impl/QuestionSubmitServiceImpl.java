package com.spingbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.judo.mq.JudgeTaskTriggerFactory;
import com.spingbootinit.mapper.QuestionSubmitMapper;
import com.spingbootinit.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.spingbootinit.model.entity.Question;
import com.spingbootinit.model.entity.QuestionSubmit;
import com.spingbootinit.model.enums.QuestionSubmitStatusEnum;
import com.spingbootinit.model.vo.questionsubmitvo.MySubmitItemVO;
import com.spingbootinit.service.ContestService;
import com.spingbootinit.service.QuestionService;
import com.spingbootinit.service.QuestionSubmitService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit> implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;

    @Resource
    private ContestService contestService;

    @Resource
    @Lazy
    private JudgeTaskTriggerFactory judgeTaskTriggerFactory;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @return 提交记录的 id
     */
    @Override
    public QuestionSubmit QuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest ,long currentUserId) {
        if (questionSubmitAddRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        Long questionId = questionSubmitAddRequest.getQuestionId();
        String language = StringUtils.trimToEmpty(questionSubmitAddRequest.getLanguage());
        String code = StringUtils.trimToEmpty(questionSubmitAddRequest.getCode());
        if (questionId == null || questionId <= 0 || StringUtils.isBlank(language) || StringUtils.isBlank(code)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "请完善题目、语言和代码");
        }
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "题目不存在");
        }
        if (question.getQuestionType() != null && question.getQuestionType().toUpperCase().startsWith("IMAGE_")
                && !"python".equalsIgnoreCase(language)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(),
                    "图像类题目目前仅支持 Python");
        }

        question.setSubmitNum(question.getSubmitNum() + 1);
        questionService.updateById(question);
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(code);
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setLanguage(language);
        questionSubmit.setUserId(currentUserId);
        questionSubmit.setJudgeInfo("{}");

        Long contestId = questionSubmitAddRequest.getContestId();
        if (contestId != null && contestId > 0) {
            contestService.assertContestSubmitAllowed(currentUserId, contestId, questionId);
            questionSubmit.setContestId(contestId);
        }

        boolean saved = this.save(questionSubmit);
        if (!saved || questionSubmit.getId() == null) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "提交失败，请稍后重试");
        }

        judgeTaskTriggerFactory.enqueue(questionSubmit.getId());


        return questionSubmit;
    }

    @Override
    public long computeSubmitSerial(QuestionSubmit submit, boolean perQuestion) {
        if (submit.getUserId() == null || submit.getId() == null) {
            return 1L;
        }
        LambdaQueryWrapper<QuestionSubmit> w = new LambdaQueryWrapper<>();
        w.eq(QuestionSubmit::getUserId, submit.getUserId());
        if (perQuestion && submit.getQuestionId() != null) {
            w.eq(QuestionSubmit::getQuestionId, submit.getQuestionId());
        }
        Date ct = submit.getCreateTime();
        if (ct == null) {
            w.le(QuestionSubmit::getId, submit.getId());
        } else {
            w.and(q -> q.lt(QuestionSubmit::getCreateTime, ct)
                    .or(q2 -> q2.eq(QuestionSubmit::getCreateTime, ct)
                            .le(QuestionSubmit::getId, submit.getId())));
        }
        return this.count(w);
    }

    @Override
    public MySubmitItemVO toMySubmitItemVo(QuestionSubmit item, boolean serialPerQuestion, boolean exposeDbSubmitId) {
        MySubmitItemVO vo = new MySubmitItemVO();
        if (exposeDbSubmitId && item.getId() != null) {
            vo.setId(String.valueOf(item.getId()));
        }
        vo.setSubmitNo(computeSubmitSerial(item, serialPerQuestion));
        vo.setGlobalSubmitNo(computeSubmitSerial(item, false));
        vo.setQuestionId(item.getQuestionId() == null ? null : String.valueOf(item.getQuestionId()));
        if (item.getQuestionId() != null) {
            Question question = questionService.getById(item.getQuestionId());
            vo.setQuestionTitle(question == null ? null : question.getTitle());
        }
        vo.setLanguage(item.getLanguage());
        vo.setStatus(item.getStatus());
        vo.setJudgeInfo(item.getJudgeInfo());
        vo.setUserId(item.getUserId() == null ? null : String.valueOf(item.getUserId()));
        vo.setContestId(item.getContestId() != null && item.getContestId() > 0
                ? String.valueOf(item.getContestId())
                : null);
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    @Override
    public MySubmitItemVO getMySubmitStatus(Long submitId, long currentUserId) {
        if (submitId == null || submitId <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "提交 id 非法");
        }
        QuestionSubmit item = this.getById(submitId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "提交记录不存在");
        }
        if (!Objects.equals(currentUserId, item.getUserId())) {
            throw new BusinessException(ResultCode.NO_AUTH_ERROR.getCode(), "无权查看该提交");
        }
        return toMySubmitItemVo(item, true, false);
    }

    @Override
    public MySubmitItemVO getMySubmitStatusBySubmitNo(long currentUserId, long questionId, long submitNo) {
        if (questionId <= 0 || submitNo < 1) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "题目或提交序号非法");
        }
        QuestionSubmit item = findByUserQuestionAndSubmitNo(currentUserId, questionId, submitNo);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "提交记录不存在");
        }
        return toMySubmitItemVo(item, true, false);
    }

    @Override
    public QuestionSubmit findByUserQuestionAndSubmitNo(long userId, long questionId, long submitNo) {
        if (userId <= 0 || questionId <= 0 || submitNo < 1) {
            return null;
        }
        long offset = submitNo - 1;
        if (offset > 10_000_000L) {
            return null;
        }
        return this.lambdaQuery()
                .eq(QuestionSubmit::getUserId, userId)
                .eq(QuestionSubmit::getQuestionId, questionId)
                .orderByAsc(QuestionSubmit::getCreateTime)
                .orderByAsc(QuestionSubmit::getId)
                .last("LIMIT 1 OFFSET " + offset)
                .one();
    }

    @Override
    public QuestionSubmit findByUserGlobalSubmitNo(long userId, long submitNo) {
        if (userId <= 0 || submitNo < 1) {
            return null;
        }
        long offset = submitNo - 1;
        if (offset > 10_000_000L) {
            return null;
        }
        return this.lambdaQuery()
                .eq(QuestionSubmit::getUserId, userId)
                .orderByAsc(QuestionSubmit::getCreateTime)
                .orderByAsc(QuestionSubmit::getId)
                .last("LIMIT 1 OFFSET " + offset)
                .one();
    }

}
