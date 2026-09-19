package com.spingbootinit.controller;

import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.Result;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.dto.contest.ContestJoinRequest;
import com.spingbootinit.model.vo.contest.ContestDetailVO;
import com.spingbootinit.model.vo.contest.ContestListItemVO;
import com.spingbootinit.model.vo.contest.ContestRankRowVO;
import com.spingbootinit.service.ContestRankService;
import com.spingbootinit.service.ContestService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contest")
public class ContestController {

    @Resource
    private ContestService contestService;

    @Resource
    private ContestRankService contestRankService;

    @GetMapping("/list")
    public Result<List<ContestListItemVO>> listContests() {
        return Result.success(contestService.listContests());
    }

    @GetMapping("/get")
    public Result<ContestDetailVO> getContest(@RequestParam("id") Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "请传入竞赛 id");
        }
        Long viewerId = UserContext.getCurrentUserId();
        return Result.success(contestService.getContestDetail(id, viewerId));
    }

    @GetMapping("/rank")
    public Result<List<ContestRankRowVO>> rank(@RequestParam("contestId") Long contestId) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "请传入 contestId");
        }
        contestService.assertRankVisible(contestId);
        return Result.success(contestRankService.listRankRows(contestId));
    }

    @PostMapping("/join")
    public Result<Boolean> join(@RequestBody ContestJoinRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        contestService.joinContest(uid, request);
        return Result.success(true);
    }
}
