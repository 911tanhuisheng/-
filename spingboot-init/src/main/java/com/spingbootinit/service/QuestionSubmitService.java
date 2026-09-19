package com.spingbootinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spingbootinit.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.spingbootinit.model.entity.QuestionSubmit;
import com.spingbootinit.model.vo.questionsubmitvo.MySubmitItemVO;

public interface QuestionSubmitService extends IService<QuestionSubmit> {

    /**
     * 计算提交序号：perQuestion=true 表示「该用户对该题目的第几次」；false 表示「该用户账号下全局第几次」（按创建时间、id 排序）。
     */
    long computeSubmitSerial(QuestionSubmit submit, boolean perQuestion);

    /**
     * 组装提交记录 VO（是否对外暴露数据库提交主键 id）。
     */
    MySubmitItemVO toMySubmitItemVo(QuestionSubmit item, boolean serialPerQuestion, boolean exposeDbSubmitId);

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @return 提交记录的 id
     */
    QuestionSubmit QuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest ,long currentUserId);

    /**
     * 查询单次提交状态（仅本人可查，供前端轮询；按数据库主键）
     */
    MySubmitItemVO getMySubmitStatus(Long submitId, long currentUserId);

    /**
     * 按「该题第几次提交」查询状态（仅本人），不暴露数据库主键给前端。
     */
    MySubmitItemVO getMySubmitStatusBySubmitNo(long currentUserId, long questionId, long submitNo);

    /**
     * 按用户 + 题目 + 次序定位一条提交（按创建时间、id 升序的第 submitNo 条），不存在则返回 null。
     */
    QuestionSubmit findByUserQuestionAndSubmitNo(long userId, long questionId, long submitNo);

    /**
     * 按用户 + 全站次序定位一条提交（该用户所有题目提交按时间、id 升序的第 submitNo 条），不存在则返回 null。
     */
    QuestionSubmit findByUserGlobalSubmitNo(long userId, long submitNo);
}
