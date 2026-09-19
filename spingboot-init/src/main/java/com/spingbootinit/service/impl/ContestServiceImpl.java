package com.spingbootinit.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.mapper.ContestMapper;
import com.spingbootinit.mapper.ContestQuestionMapper;
import com.spingbootinit.mapper.ContestRankMapper;
import com.spingbootinit.mapper.ContestUserMapper;
import com.spingbootinit.model.dto.contest.ContestAdminAddRequest;
import com.spingbootinit.model.dto.contest.ContestAdminBatchDeleteRequest;
import com.spingbootinit.model.dto.contest.ContestAdminPageRequest;
import com.spingbootinit.model.dto.contest.ContestAdminUpdateRequest;
import com.spingbootinit.model.dto.contest.ContestJoinRequest;
import com.spingbootinit.model.dto.contest.ContestQuestionUpsertItem;
import com.spingbootinit.model.entity.Contest;
import com.spingbootinit.model.entity.ContestQuestion;
import com.spingbootinit.model.entity.ContestRank;
import com.spingbootinit.model.entity.ContestUser;
import com.spingbootinit.model.entity.Question;
import com.spingbootinit.model.entity.UserInAppNotification;
import com.spingbootinit.model.vo.contest.ContestAdminListItemVO;
import com.spingbootinit.model.vo.contest.ContestDetailVO;
import com.spingbootinit.model.vo.contest.ContestListItemVO;
import com.spingbootinit.model.vo.contest.ContestQuestionBriefVO;
import com.spingbootinit.service.ContestService;
import com.spingbootinit.service.InAppNotificationService;
import com.spingbootinit.service.QuestionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContestServiceImpl extends ServiceImpl<ContestMapper, Contest> implements ContestService {

    @Resource
    private ContestUserMapper contestUserMapper;

    @Resource
    private ContestQuestionMapper contestQuestionMapper;

    @Resource
    private QuestionService questionService;

    @Resource
    private ContestRankMapper contestRankMapper;

    @Resource
    private InAppNotificationService inAppNotificationService;

    private static String phaseOf(Date now, Date start, Date end) {
        if (now.before(start)) {
            return "未开始";
        }
        if (now.after(end)) {
            return "已结束";
        }
        return "进行中";
    }

    @Override
    public List<ContestListItemVO> listContests() {
        Date now = new Date();
        LambdaQueryWrapper<Contest> w = new LambdaQueryWrapper<>();
        w.orderByDesc(Contest::getStartTime).orderByDesc(Contest::getId);
        return this.list(w).stream().map(c -> {
            ContestListItemVO vo = new ContestListItemVO();
            vo.setId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setStartTime(c.getStartTime());
            vo.setEndTime(c.getEndTime());
            vo.setPhase(phaseOf(now, c.getStartTime(), c.getEndTime()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ContestDetailVO getContestDetail(long contestId, Long viewerUserId) {
        Contest c = this.getById(contestId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "竞赛不存在");
        }
        Date now = new Date();
        ContestDetailVO vo = new ContestDetailVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setDescription(c.getDescription());
        vo.setStartTime(c.getStartTime());
        vo.setEndTime(c.getEndTime());
        vo.setPhase(phaseOf(now, c.getStartTime(), c.getEndTime()));
        if (viewerUserId != null && viewerUserId > 0) {
            LambdaQueryWrapper<ContestUser> reg = new LambdaQueryWrapper<>();
            reg.eq(ContestUser::getContestId, contestId).eq(ContestUser::getUserId, viewerUserId);
            vo.setMeRegistered(contestUserMapper.selectCount(reg) > 0);
        } else {
            vo.setMeRegistered(null);
        }

        LambdaQueryWrapper<ContestQuestion> cq = new LambdaQueryWrapper<>();
        cq.eq(ContestQuestion::getContestId, contestId)
                .orderByAsc(ContestQuestion::getSortOrder)
                .orderByAsc(ContestQuestion::getId);
        List<ContestQuestionBriefVO> qs = contestQuestionMapper.selectList(cq).stream().map(row -> {
            ContestQuestionBriefVO b = new ContestQuestionBriefVO();
            b.setQuestionId(row.getQuestionId());
            b.setFullScore(row.getFullScore());
            b.setSortOrder(row.getSortOrder());
            Question q = questionService.getById(row.getQuestionId());
            b.setQuestionTitle(q != null ? q.getTitle() : "（题目已删除）");
            return b;
        }).collect(Collectors.toList());
        vo.setQuestions(qs);
        return vo;
    }

    @Override
    public void joinContest(long userId, ContestJoinRequest request) {
        if (request == null || request.getContestId() == null || request.getContestId() <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "请指定竞赛 ID");
        }
        long contestId = request.getContestId();
        Contest c = this.getById(contestId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "竞赛不存在");
        }
        Date now = new Date();
        if (now.before(c.getStartTime())) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "比赛尚未开始，暂不可报名");
        }
        if (!now.before(c.getEndTime())) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "比赛已结束，无法报名");
        }
        LambdaQueryWrapper<ContestUser> w = new LambdaQueryWrapper<>();
        w.eq(ContestUser::getContestId, contestId).eq(ContestUser::getUserId, userId);
        if (contestUserMapper.selectCount(w) > 0) {
            return;
        }
        ContestUser cu = new ContestUser();
        cu.setContestId(contestId);
        cu.setUserId(userId);
        contestUserMapper.insert(cu);
        String title = c.getTitle() == null ? "比赛" : c.getTitle();
        inAppNotificationService.send(
                userId,
                UserInAppNotification.TYPE_CONTEST_JOIN,
                "报名成功",
                "已成功报名「" + title + "」，开赛前将收到提醒。",
                "CONTEST",
                String.valueOf(contestId),
                "CONTEST_JOIN_" + contestId + "_" + userId);
    }

    @Override
    public void assertContestSubmitAllowed(long userId, long contestId, long questionId) {
        Contest c = this.getById(contestId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "竞赛不存在");
        }
        Date now = new Date();
        if (now.before(c.getStartTime())) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "比赛尚未开始，无法提交");
        }
        if (now.after(c.getEndTime())) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "比赛已结束，无法提交");
        }
        LambdaQueryWrapper<ContestUser> uw = new LambdaQueryWrapper<>();
        uw.eq(ContestUser::getContestId, contestId).eq(ContestUser::getUserId, userId);
        if (contestUserMapper.selectCount(uw) <= 0) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "请先报名该竞赛后再提交");
        }
        LambdaQueryWrapper<ContestQuestion> qw = new LambdaQueryWrapper<>();
        qw.eq(ContestQuestion::getContestId, contestId).eq(ContestQuestion::getQuestionId, questionId);
        if (contestQuestionMapper.selectCount(qw) <= 0) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "该题目不属于此竞赛");
        }
    }

    @Override
    public void assertRankVisible(long contestId) {
        Contest c = this.getById(contestId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "竞赛不存在");
        }
        Date now = new Date();
        if (now.before(c.getStartTime())) {
            throw new BusinessException(ResultCode.FORBIDDEN_ERROR.getCode(), "比赛未开始，排行榜暂不可见");
        }
    }

    @Override
    public Page<ContestAdminListItemVO> pageAdminContests(ContestAdminPageRequest request) {
        long pageNo = request.getCurrent() > 0 ? request.getCurrent() : 1;
        long size = request.getPageSize() > 0 ? request.getPageSize() : 10;
        Page<Contest> page = new Page<>(pageNo, size);
        LambdaQueryWrapper<Contest> w = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(request.getTitle())) {
            w.like(Contest::getTitle, request.getTitle().trim());
        }
        w.orderByDesc(Contest::getStartTime).orderByDesc(Contest::getId);
        Page<Contest> raw = this.page(page, w);
        Date now = new Date();
        List<Long> ids = raw.getRecords().stream().map(Contest::getId).toList();
        Map<Long, Integer> qCount = new HashMap<>();
        if (!ids.isEmpty()) {
            LambdaQueryWrapper<ContestQuestion> cq = new LambdaQueryWrapper<>();
            cq.in(ContestQuestion::getContestId, ids);
            for (ContestQuestion row : contestQuestionMapper.selectList(cq)) {
                qCount.merge(row.getContestId(), 1, Integer::sum);
            }
        }
        Page<ContestAdminListItemVO> voPage = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        voPage.setRecords(raw.getRecords().stream().map(c -> {
            ContestAdminListItemVO vo = new ContestAdminListItemVO();
            vo.setId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setStartTime(c.getStartTime());
            vo.setEndTime(c.getEndTime());
            vo.setPhase(phaseOf(now, c.getStartTime(), c.getEndTime()));
            vo.setQuestionCount(qCount.getOrDefault(c.getId(), 0));
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminAddContest(ContestAdminAddRequest request) {
        assertTimeRange(request.getStartTime(), request.getEndTime());
        validateQuestionItems(request.getQuestions());
        Contest c = new Contest();
        c.setTitle(request.getTitle().trim());
        c.setDescription(StrUtil.nullToDefault(request.getDescription(), "").trim());
        if (c.getDescription().isEmpty()) {
            c.setDescription(null);
        }
        c.setStartTime(Date.from(request.getStartTime()));
        c.setEndTime(Date.from(request.getEndTime()));
        boolean ok = this.save(c);
        if (!ok || c.getId() == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "创建赛事失败");
        }
        insertContestQuestions(c.getId(), request.getQuestions());
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdateContest(ContestAdminUpdateRequest request) {
        Contest existing = this.getById(request.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "赛事不存在");
        }
        assertTimeRange(request.getStartTime(), request.getEndTime());
        validateQuestionItems(request.getQuestions());
        existing.setTitle(request.getTitle().trim());
        String desc = StrUtil.nullToDefault(request.getDescription(), "").trim();
        existing.setDescription(desc.isEmpty() ? null : desc);
        existing.setStartTime(Date.from(request.getStartTime()));
        existing.setEndTime(Date.from(request.getEndTime()));
        boolean ok = this.updateById(existing);
        if (!ok) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "更新赛事失败");
        }
        contestQuestionMapper.physicalDeleteByContestId(request.getId());
        insertContestQuestions(request.getId(), request.getQuestions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteContests(ContestAdminBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return;
        }
        for (Long id : request.getIds()) {
            if (id == null || id <= 0) {
                continue;
            }
            Contest c = this.getById(id);
            if (c == null) {
                continue;
            }
            LambdaQueryWrapper<ContestQuestion> q1 = new LambdaQueryWrapper<>();
            q1.eq(ContestQuestion::getContestId, id);
            contestQuestionMapper.delete(q1);
            LambdaQueryWrapper<ContestUser> q2 = new LambdaQueryWrapper<>();
            q2.eq(ContestUser::getContestId, id);
            contestUserMapper.delete(q2);
            LambdaQueryWrapper<ContestRank> q3 = new LambdaQueryWrapper<>();
            q3.eq(ContestRank::getContestId, id);
            contestRankMapper.delete(q3);
            this.removeById(id);
        }
    }

    private static void assertTimeRange(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "开始时间与结束时间不能为空");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "结束时间必须晚于开始时间");
        }
    }

    private void validateQuestionItems(List<ContestQuestionUpsertItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (ContestQuestionUpsertItem it : items) {
            long qid = parseQuestionId(it.getQuestionId());
            if (!seen.add(qid)) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "赛题列表中存在重复的题目");
            }
            Question q = questionService.getById(qid);
            if (q == null) {
                throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "题目不存在：" + qid);
            }
        }
    }

    private static long parseQuestionId(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "题目 id 不能为空");
        }
        String s = raw.trim();
        try {
            long qid = Long.parseLong(s);
            if (qid <= 0) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "题目 id 非法");
            }
            return qid;
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "题目 id 格式错误");
        }
    }

    private void insertContestQuestions(Long contestId, List<ContestQuestionUpsertItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (int idx = 0; idx < items.size(); idx++) {
            ContestQuestionUpsertItem it = items.get(idx);
            ContestQuestion row = new ContestQuestion();
            row.setContestId(contestId);
            row.setQuestionId(parseQuestionId(it.getQuestionId()));
            row.setFullScore(it.getFullScore() != null ? it.getFullScore() : 100);
            row.setSortOrder(it.getSortOrder() != null ? it.getSortOrder() : (idx + 1));
            contestQuestionMapper.insert(row);
        }
    }
}
