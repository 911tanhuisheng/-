package com.spingbootinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spingbootinit.model.entity.ContestRank;
import com.spingbootinit.model.vo.contest.ContestRankRowVO;

import java.util.List;

public interface ContestRankService extends IService<ContestRank> {

    /**
     * 判题结果已写入 question_submit 后调用；独立事务，失败不影响判题主事务。
     */
    void onJudgeFinished(Long questionSubmitId);

    List<ContestRankRowVO> listRankRows(long contestId);
}
