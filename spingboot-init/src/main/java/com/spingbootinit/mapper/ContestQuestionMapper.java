package com.spingbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spingbootinit.model.entity.ContestQuestion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface ContestQuestionMapper extends BaseMapper<ContestQuestion> {

    /**
     * 维护赛题列表时必须物理删除：逻辑删除后唯一索引 uk_contest_question(contest_id, question_id) 仍占用，无法再插入相同组合。
     */
    @Delete("DELETE FROM contest_question WHERE contest_id = #{contestId}")
    int physicalDeleteByContestId(@Param("contestId") Long contestId);
}
