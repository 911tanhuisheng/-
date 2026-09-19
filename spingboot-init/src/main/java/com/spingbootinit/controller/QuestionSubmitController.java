package com.spingbootinit.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.Result;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.IsAdminRoleString;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.spingbootinit.model.entity.QuestionSubmit;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.model.vo.questionsubmitvo.MySubmitItemVO;
import com.spingbootinit.model.vo.questionsubmitvo.QuestionSubmitCreatedVO;
import com.spingbootinit.service.QuestionSubmitService;
import com.spingbootinit.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目提交接口
 */
@RestController
@RequestMapping("/question_submit")
@Slf4j
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserService userService;

    @Resource
    private IsAdminRoleString isAdminRole;

    /**
     * 提交题目
     *
     * @return 含题目 id、本题次序 submitNo、全站次序 globalSubmitNo（与「全部提交」列表行一致）；管理员额外返回 dbId。轮询用 GET /question_submit/status/my。
     */
    @PostMapping("/")
    public Result<QuestionSubmitCreatedVO> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        QuestionSubmit submit = questionSubmitService.QuestionSubmit(questionSubmitAddRequest, currentUserId);
        QuestionSubmitCreatedVO vo = new QuestionSubmitCreatedVO();
        vo.setQuestionId(submit.getQuestionId());
        vo.setSubmitNo(questionSubmitService.computeSubmitSerial(submit, true));
        vo.setGlobalSubmitNo(questionSubmitService.computeSubmitSerial(submit, false));
        User me = userService.getById(currentUserId);
        if (me != null && isAdminRole.isAdminRoleString(me.getUserRole())) {
            vo.setDbId(submit.getId() == null ? null : String.valueOf(submit.getId()));
        }
        return Result.success(vo);
    }

    /**
     * 按「该题第几次提交」查询状态（推荐：响应体不含数据库提交主键）。
     */
    @GetMapping("/status/my")
    public Result<MySubmitItemVO> getSubmitStatusBySubmitNo(
            @RequestParam("questionId") Long questionId,
            @RequestParam("submitNo") Long submitNo
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(questionSubmitService.getMySubmitStatusBySubmitNo(currentUserId, questionId, submitNo));
    }

    /**
     * 按数据库主键查询单次提交状态（兼容旧前端；响应体仍不返回 id，请用 submitNo 展示）。
     */
    @GetMapping("/status/{id}")
    public Result<MySubmitItemVO> getSubmitStatus(@PathVariable("id") Long submitId) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(questionSubmitService.getMySubmitStatus(submitId, currentUserId));
    }

    @GetMapping("/my/page/all")
    public Result<Page<MySubmitItemVO>> pageMyAllQuestionSubmits(
            @RequestParam(value = "submitNo", required = false) Long submitNo,
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        long pageNo = current <= 0 ? 1 : current;
        long size = pageSize <= 0 ? 10 : Math.min(pageSize, 50);

        if (submitNo != null && submitNo > 0) {
            QuestionSubmit one = questionSubmitService.findByUserGlobalSubmitNo(currentUserId, submitNo);
            if (one == null) {
                Page<MySubmitItemVO> empty = new Page<>(1, size, 0);
                empty.setRecords(List.of());
                return Result.success(empty);
            }
            MySubmitItemVO vo = questionSubmitService.toMySubmitItemVo(one, false, false);
            Page<MySubmitItemVO> single = new Page<>(1, size, 1);
            single.setRecords(List.of(vo));
            return Result.success(single);
        }

        Page<QuestionSubmit> page = questionSubmitService.lambdaQuery()
                .eq(QuestionSubmit::getUserId, currentUserId)
                .orderByAsc(QuestionSubmit::getCreateTime)
                .orderByAsc(QuestionSubmit::getId)
                .page(new Page<>(pageNo, size));

        List<MySubmitItemVO> records = page.getRecords().stream()
                .map(item -> questionSubmitService.toMySubmitItemVo(item, false, false))
                .toList();

        Page<MySubmitItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @GetMapping("/admin/page")
    public Result<Page<MySubmitItemVO>> pageAllQuestionSubmitsForAdmin(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "questionId", required = false) Long questionId,
            @RequestParam(value = "submitNo", required = false) Long submitNo
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || !isAdminRole.isAdminRoleString(currentUser.getUserRole())) {
            throw new BusinessException(ResultCode.NO_AUTH_ERROR);
        }
        long pageNo = current <= 0 ? 1 : current;
        long size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);

        if (submitNo != null && submitNo > 0) {
            if (userId == null || userId <= 0 || questionId == null || questionId <= 0) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "按次序筛选需同时填写用户 ID 与题目 ID");
            }
            QuestionSubmit one = questionSubmitService.findByUserQuestionAndSubmitNo(userId, questionId, submitNo);
            if (one == null) {
                Page<MySubmitItemVO> empty = new Page<>(1, size, 0);
                empty.setRecords(List.of());
                return Result.success(empty);
            }
            MySubmitItemVO vo = questionSubmitService.toMySubmitItemVo(one, true, true);
            Page<MySubmitItemVO> single = new Page<>(1, size, 1);
            single.setRecords(List.of(vo));
            return Result.success(single);
        }

        var query = questionSubmitService.lambdaQuery();
        if (userId != null && userId > 0) {
            query.eq(QuestionSubmit::getUserId, userId);
        }
        if (questionId != null && questionId > 0) {
            query.eq(QuestionSubmit::getQuestionId, questionId);
        }
        Page<QuestionSubmit> page = query
                .orderByAsc(QuestionSubmit::getCreateTime)
                .orderByAsc(QuestionSubmit::getId)
                .page(new Page<>(pageNo, size));

        List<MySubmitItemVO> records = page.getRecords().stream()
                .map(item -> questionSubmitService.toMySubmitItemVo(item, true, true))
                .toList();

        Page<MySubmitItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

}
