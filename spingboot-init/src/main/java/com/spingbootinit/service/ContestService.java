package com.spingbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spingbootinit.model.dto.contest.ContestAdminAddRequest;
import com.spingbootinit.model.dto.contest.ContestAdminBatchDeleteRequest;
import com.spingbootinit.model.dto.contest.ContestAdminPageRequest;
import com.spingbootinit.model.dto.contest.ContestAdminUpdateRequest;
import com.spingbootinit.model.dto.contest.ContestJoinRequest;
import com.spingbootinit.model.vo.contest.ContestAdminListItemVO;
import com.spingbootinit.model.vo.contest.ContestDetailVO;
import com.spingbootinit.model.vo.contest.ContestListItemVO;

import java.util.List;

public interface ContestService {

    List<ContestListItemVO> listContests();

    /**
     * @param viewerUserId 当前登录用户 id；未登录传 null，则不填充 meRegistered
     */
    ContestDetailVO getContestDetail(long contestId, Long viewerUserId);

    void joinContest(long userId, ContestJoinRequest request);

    /**
     * 校验：已报名、题目属于赛、在时间窗内
     */
    void assertContestSubmitAllowed(long userId, long contestId, long questionId);

    /**
     * 排行榜可见性：开赛后才可查看（与常见 OJ 一致）
     */
    void assertRankVisible(long contestId);

    Page<ContestAdminListItemVO> pageAdminContests(ContestAdminPageRequest request);

    Long adminAddContest(ContestAdminAddRequest request);

    void adminUpdateContest(ContestAdminUpdateRequest request);

    void adminDeleteContests(ContestAdminBatchDeleteRequest request);
}
