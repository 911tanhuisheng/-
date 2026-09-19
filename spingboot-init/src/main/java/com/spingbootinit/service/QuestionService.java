package com.spingbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.spingbootinit.model.dto.question.QuestionAddRequest;
import com.spingbootinit.model.dto.question.QuestionBatchDeleteRequest;
import com.spingbootinit.model.dto.question.QuestionQueryRequest;
import com.spingbootinit.model.dto.question.QuestionUpdateRequest;
import com.spingbootinit.model.entity.Question;
import com.spingbootinit.model.vo.questionvo.QuestionAdminListItemVO;
import com.spingbootinit.model.vo.questionvo.QuestionPublicDetailVO;
import jakarta.validation.Valid;

public interface QuestionService extends IService<Question> {
    /**
     * 题目业务校验。
     *
     * @param question 题目实体
     * @param add 是否为新增场景
     */
    void validQuestion(Question question, boolean add);


    /**
     * 创建题目
     * @param questionAddRequest
     * @param currentUserId
     * @return
     */
    long createQuestion(@Valid QuestionAddRequest questionAddRequest, Long currentUserId);


    /**
     * 分页查询题目列表 仅管理员
     * @param queryRequest
     * @return
     */
    Page<QuestionAdminListItemVO> pageSelect(QuestionQueryRequest queryRequest);

    /**
     * 分页查询题目列表 公开
     * @param queryRequest
     * @return
     */
    Page<QuestionAdminListItemVO> pageSelectPublic(QuestionQueryRequest queryRequest);

    /**
     * 获取题目详情 仅公开
     * @param id
     * @return
     */
    QuestionPublicDetailVO getByIdPublicDetail(Long id);

    /**
     * 更新题目
     */
    void updateByIdQuestion(@Valid QuestionUpdateRequest request);

    /**
     * 批量删除题目
     */
    void removeBatchByIdsQuestion(@Valid QuestionBatchDeleteRequest request);
}
