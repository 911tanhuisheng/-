package com.spingbootinit.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spingbootinit.mapper.ContestMapper;
import com.spingbootinit.mapper.ContestQuestionMapper;
import com.spingbootinit.mapper.ContestRankMapper;
import com.spingbootinit.mapper.ContestUserMapper;
import com.spingbootinit.model.dto.contest.ContestDetailUnit;
import com.spingbootinit.model.dto.questionsubmit.JudgeInfo;
import com.spingbootinit.model.entity.*;
import com.spingbootinit.model.enums.JudgeInfoMessageEnum;
import com.spingbootinit.model.enums.QuestionSubmitStatusEnum;
import com.spingbootinit.model.vo.contest.ContestRankCellVO;
import com.spingbootinit.model.vo.contest.ContestRankRowVO;
import com.spingbootinit.service.ContestRankService;
import com.spingbootinit.service.QuestionSubmitService;
import com.spingbootinit.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 竞赛榜：ICPC 常见规则（与 Codeforces Gym / 国内训练赛接近）<br>
 * 排序：通过题数降序 → 总罚时升序 → 总分（备用）→ userId 升序<br>
 * 单题罚时：开赛至「首次 AC」的整分钟数 + 20×「首次 AC 前」非编译错误的失败次数；编译错误不计入 +20。<br>
 * 「排名」列：按上述规则全序排序后的<strong>连续名次</strong> 1…n（与表格行顺序一致），避免仅按通过/罚时并列导致与行顺序观感不一致。
 */
@Service
@Slf4j
public class ContestRankServiceImpl extends ServiceImpl<ContestRankMapper, ContestRank> implements ContestRankService {

    private static final int WRONG_SUBMISSION_PENALTY_MINUTES = 20;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private ContestQuestionMapper contestQuestionMapper;

    @Resource
    private ContestUserMapper contestUserMapper;

    @Resource
    private ContestMapper contestMapper;

    @Resource
    private UserService userService;

    private static int compareRankRows(ContestRankRowVO a, ContestRankRowVO b) {
        int c = Integer.compare(
                Objects.requireNonNullElse(b.getSolvedCount(), 0),
                Objects.requireNonNullElse(a.getSolvedCount(), 0));
        if (c != 0) {
            return c;
        }
        c = Integer.compare(
                Objects.requireNonNullElse(a.getTotalPenalty(), 0),
                Objects.requireNonNullElse(b.getTotalPenalty(), 0));
        if (c != 0) {
            return c;
        }
        c = Integer.compare(
                Objects.requireNonNullElse(b.getTotalScore(), 0),
                Objects.requireNonNullElse(a.getTotalScore(), 0));
        if (c != 0) {
            return c;
        }
        return Long.compare(
                Objects.requireNonNullElse(a.getUserId(), 0L),
                Objects.requireNonNullElse(b.getUserId(), 0L));
    }

    private static Map<Long, ContestDetailUnit> parseDetail(String json) {
        Map<Long, ContestDetailUnit> map = new HashMap<>();
        if (StrUtil.isBlank(json)) {
            return map;
        }
        String t = json.trim();
        if ("{}".equals(t)) {
            return map;
        }
        try {
            JSONObject root = JSONUtil.parseObj(json);
            for (String key : root.keySet()) {
                long qid = Long.parseLong(key);
                cn.hutool.json.JSONObject sub = root.getJSONObject(key);
                if (sub != null) {
                    map.put(qid, JSONUtil.toBean(sub, ContestDetailUnit.class));
                }
            }
        } catch (Exception e) {
            log.warn("解析 game_detail 失败: {}", e.getMessage());
        }
        return map;
    }

    private static String toDetailJson(Map<Long, ContestDetailUnit> map) {
        JSONObject o = new JSONObject();
        for (Map.Entry<Long, ContestDetailUnit> e : map.entrySet()) {
            o.set(String.valueOf(e.getKey()), JSONUtil.parseObj(JSONUtil.toJsonStr(e.getValue())));
        }
        return o.toString();
    }

    private static void recomputeTotals(ContestRank rank, Map<Long, ContestDetailUnit> map) {
        int ts = 0;
        long tt = 0;
        long tm = 0;
        for (ContestDetailUnit u : map.values()) {
            ts += u.getScore() == null ? 0 : u.getScore();
            tt += u.getTime() == null ? 0L : u.getTime();
            tm += u.getMemory() == null ? 0L : u.getMemory();
        }
        rank.setTotalScore(ts);
        rank.setTotalTime(tt);
        rank.setTotalMemory(tm);
        rank.setGameDetail(toDetailJson(map));
    }

    private static String displayName(User u) {
        if (u == null) {
            return "用户";
        }
        if (StrUtil.isNotBlank(u.getNickname())) {
            return u.getNickname().trim();
        }
        return StrUtil.blankToDefault(u.getUsername(), "用户");
    }

    private static JudgeInfo parseJudgeInfo(QuestionSubmit s) {
        if (s == null || StrUtil.isBlank(s.getJudgeInfo())) {
            return null;
        }
        try {
            return JSONUtil.toBean(s.getJudgeInfo(), JudgeInfo.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static String messageOf(QuestionSubmit s) {
        JudgeInfo ji = parseJudgeInfo(s);
        return ji != null ? StrUtil.nullToEmpty(ji.getMessage()) : "";
    }

    private static boolean isAcceptedSubmit(QuestionSubmit s) {
        if (s == null || !Objects.equals(s.getStatus(), QuestionSubmitStatusEnum.SUCCEED.getValue())) {
            return false;
        }
        return JudgeInfoMessageEnum.ACCEPTED.getValue().equals(messageOf(s));
    }

    private static boolean isCompileErrorSubmit(QuestionSubmit s) {
        return JudgeInfoMessageEnum.COMPILE_ERROR.getValue().equals(messageOf(s));
    }

    /**
     * 根据该题全部已终态提交，按 ICPC 规则重算本题单元。
     */
    private ContestDetailUnit computeIcpcUnit(long contestId, long userId, long questionId, int fullScore, Date contestStart) {
        LambdaQueryWrapper<QuestionSubmit> w = new LambdaQueryWrapper<>();
        w.eq(QuestionSubmit::getContestId, contestId)
                .eq(QuestionSubmit::getUserId, userId)
                .eq(QuestionSubmit::getQuestionId, questionId)
                .in(QuestionSubmit::getStatus, List.of(
                        QuestionSubmitStatusEnum.SUCCEED.getValue(),
                        QuestionSubmitStatusEnum.FAILED.getValue()))
                .orderByAsc(QuestionSubmit::getCreateTime)
                .orderByAsc(QuestionSubmit::getId);
        List<QuestionSubmit> list = questionSubmitService.list(w);

        ContestDetailUnit unit = new ContestDetailUnit();
        unit.setQuestionId(questionId);
        unit.setTotalSubmissions(list.size());

        if (list.isEmpty()) {
            unit.setScore(0);
            unit.setPenaltyMinutes(0);
            unit.setAttempts(0);
            unit.setWrongBeforeAc(0);
            unit.setTime(0L);
            unit.setMemory(0L);
            unit.setMessage(null);
            return unit;
        }

        int firstAcIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (isAcceptedSubmit(list.get(i))) {
                firstAcIdx = i;
                break;
            }
        }

        if (firstAcIdx < 0) {
            unit.setScore(0);
            unit.setPenaltyMinutes(0);
            unit.setAttempts(list.size());
            int wrong = 0;
            for (QuestionSubmit s : list) {
                if (!isCompileErrorSubmit(s) && !isAcceptedSubmit(s)) {
                    wrong++;
                }
            }
            unit.setWrongBeforeAc(wrong);
            unit.setMessage(messageOf(list.get(list.size() - 1)));
            unit.setTime(0L);
            unit.setMemory(0L);
            return unit;
        }

        int wrong = 0;
        for (int j = 0; j < firstAcIdx; j++) {
            QuestionSubmit s = list.get(j);
            if (isCompileErrorSubmit(s)) {
                continue;
            }
            if (!isAcceptedSubmit(s)) {
                wrong++;
            }
        }

        QuestionSubmit firstAc = list.get(firstAcIdx);
        JudgeInfo ji = parseJudgeInfo(firstAc);
        long startMs = contestStart != null ? contestStart.getTime() : 0L;
        long acMs = firstAc.getCreateTime() != null ? firstAc.getCreateTime().getTime() : startMs;
        long elapsed = Math.max(0L, acMs - startMs);
        int baseMinutes = (int) (elapsed / 60000L);
        int penalty = baseMinutes + WRONG_SUBMISSION_PENALTY_MINUTES * wrong;

        unit.setScore(fullScore);
        unit.setMessage(JudgeInfoMessageEnum.ACCEPTED.getValue());
        unit.setTime(ji != null && ji.getTime() != null ? ji.getTime() : 0L);
        unit.setMemory(ji != null && ji.getMemory() != null ? ji.getMemory() : 0L);
        unit.setPenaltyMinutes(penalty);
        unit.setWrongBeforeAc(wrong);
        unit.setAttempts(firstAcIdx + 1);
        return unit;
    }

    private static boolean isProblemSolved(ContestDetailUnit u, int fullScore) {
        if (u == null) {
            return false;
        }
        int fs = fullScore > 0 ? fullScore : 100;
        return JudgeInfoMessageEnum.ACCEPTED.getValue().equals(StrUtil.nullToEmpty(u.getMessage()))
                && u.getScore() != null && u.getScore() >= fs;
    }

    private static String buildCellLabel(ContestDetailUnit u, int fullScore) {
        if (u == null) {
            return "—";
        }
        if (isProblemSolved(u, fullScore)) {
            int att = u.getAttempts() == null ? 1 : u.getAttempts();
            int pen = u.getPenaltyMinutes() == null ? 0 : u.getPenaltyMinutes();
            return att + "/" + pen;
        }
        int tries = u.getTotalSubmissions() != null ? u.getTotalSubmissions() : (u.getAttempts() == null ? 0 : u.getAttempts());
        if (tries > 0) {
            return "-" + tries;
        }
        return "—";
    }

    private static void fillRowAggregates(ContestRankRowVO row, Map<Long, ContestDetailUnit> detail, List<ContestQuestion> questions) {
        int solved = 0;
        int pen = 0;
        for (ContestQuestion q : questions) {
            ContestDetailUnit u = detail.get(q.getQuestionId());
            int fs = q.getFullScore() == null ? 100 : q.getFullScore();
            if (isProblemSolved(u, fs)) {
                solved++;
            }
            if (u != null && u.getPenaltyMinutes() != null) {
                pen += u.getPenaltyMinutes();
            }
        }
        row.setSolvedCount(solved);
        row.setTotalPenalty(pen);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onJudgeFinished(Long questionSubmitId) {
        if (questionSubmitId == null || questionSubmitId <= 0) {
            return;
        }
        QuestionSubmit submit = questionSubmitService.getById(questionSubmitId);
        if (submit == null || submit.getContestId() == null || submit.getContestId() <= 0) {
            return;
        }
        long contestId = submit.getContestId();
        long questionId = submit.getQuestionId();
        long userId = submit.getUserId();

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null || contest.getStartTime() == null) {
            log.warn("竞赛 {} 不存在或缺少 start_time，跳过榜更新", contestId);
            return;
        }

        LambdaQueryWrapper<ContestQuestion> cq = new LambdaQueryWrapper<>();
        cq.eq(ContestQuestion::getContestId, contestId).eq(ContestQuestion::getQuestionId, questionId);
        ContestQuestion cqRow = contestQuestionMapper.selectOne(cq);
        if (cqRow == null) {
            log.debug("提交 {} 的题目不在竞赛 {} 中，跳过榜更新", questionSubmitId, contestId);
            return;
        }
        int fullScore = cqRow.getFullScore() == null ? 100 : cqRow.getFullScore();

        LambdaQueryWrapper<ContestUser> uw = new LambdaQueryWrapper<>();
        uw.eq(ContestUser::getContestId, contestId).eq(ContestUser::getUserId, userId);
        if (contestUserMapper.selectCount(uw) <= 0) {
            log.debug("用户 {} 未报名竞赛 {}，跳过榜更新", userId, contestId);
            return;
        }

        ContestDetailUnit unit = computeIcpcUnit(contestId, userId, questionId, fullScore, contest.getStartTime());

        LambdaQueryWrapper<ContestRank> rw = new LambdaQueryWrapper<>();
        rw.eq(ContestRank::getContestId, contestId).eq(ContestRank::getUserId, userId);
        ContestRank rank = this.getOne(rw);
        User user = userService.getById(userId);
        String uname = displayName(user);

        if (rank == null) {
            rank = new ContestRank();
            rank.setContestId(contestId);
            rank.setUserId(userId);
            rank.setUserName(uname);
            Map<Long, ContestDetailUnit> map = new HashMap<>();
            map.put(questionId, unit);
            recomputeTotals(rank, map);
            this.save(rank);
            return;
        }

        if (StrUtil.isBlank(rank.getUserName())) {
            rank.setUserName(uname);
        }
        Map<Long, ContestDetailUnit> map = parseDetail(rank.getGameDetail());
        map.put(questionId, unit);
        recomputeTotals(rank, map);
        this.updateById(rank);
    }

    @Override
    public List<ContestRankRowVO> listRankRows(long contestId) {
        Contest contestMeta = contestMapper.selectById(contestId);
        Date contestStart = contestMeta != null ? contestMeta.getStartTime() : null;

        LambdaQueryWrapper<ContestQuestion> qOrder = new LambdaQueryWrapper<>();
        qOrder.eq(ContestQuestion::getContestId, contestId)
                .orderByAsc(ContestQuestion::getSortOrder)
                .orderByAsc(ContestQuestion::getId);
        List<ContestQuestion> questions = contestQuestionMapper.selectList(qOrder);

        LambdaQueryWrapper<ContestUser> cuw = new LambdaQueryWrapper<>();
        cuw.eq(ContestUser::getContestId, contestId);
        List<ContestUser> users = contestUserMapper.selectList(cuw);

        List<ContestRankRowVO> rows = new ArrayList<>();
        for (ContestUser cu : users) {
            User u = userService.getById(cu.getUserId());
            String name = displayName(u);

            Map<Long, ContestDetailUnit> detail = new HashMap<>();
            if (contestStart != null) {
                for (ContestQuestion q : questions) {
                    int fs = q.getFullScore() == null ? 100 : q.getFullScore();
                    detail.put(q.getQuestionId(), computeIcpcUnit(contestId, cu.getUserId(), q.getQuestionId(), fs, contestStart));
                }
            }

            ContestRankRowVO row = new ContestRankRowVO();
            row.setUserId(cu.getUserId());
            row.setUserName(name);
            int ts = 0;
            long tt = 0L;
            long tm = 0L;
            for (ContestDetailUnit unit : detail.values()) {
                ts += unit.getScore() == null ? 0 : unit.getScore();
                tt += unit.getTime() == null ? 0L : unit.getTime();
                tm += unit.getMemory() == null ? 0L : unit.getMemory();
            }
            row.setTotalScore(ts);
            row.setTotalTime(tt);
            row.setTotalMemory(tm);

            fillRowAggregates(row, detail, questions);

            List<ContestRankCellVO> cells = new ArrayList<>();
            for (ContestQuestion q : questions) {
                ContestDetailUnit unit = detail.get(q.getQuestionId());
                int fs = q.getFullScore() == null ? 100 : q.getFullScore();
                ContestRankCellVO cell = new ContestRankCellVO();
                cell.setQuestionId(q.getQuestionId());
                if (unit != null) {
                    cell.setScore(unit.getScore());
                    cell.setTime(unit.getTime());
                    cell.setMemory(unit.getMemory());
                    cell.setMessage(unit.getMessage());
                    cell.setPenaltyMinutes(unit.getPenaltyMinutes());
                    cell.setAttempts(unit.getAttempts());
                    cell.setTotalSubmissions(unit.getTotalSubmissions());
                    cell.setCellLabel(buildCellLabel(unit, fs));
                } else {
                    cell.setScore(0);
                    cell.setTime(0L);
                    cell.setMemory(0L);
                    cell.setMessage(null);
                    cell.setPenaltyMinutes(0);
                    cell.setAttempts(0);
                    cell.setTotalSubmissions(0);
                    cell.setCellLabel("—");
                }
                cells.add(cell);
            }
            row.setCells(cells);
            rows.add(row);
        }

        rows.sort(ContestRankServiceImpl::compareRankRows);

        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRankOrder(i + 1);
        }
        return rows;
    }
}
