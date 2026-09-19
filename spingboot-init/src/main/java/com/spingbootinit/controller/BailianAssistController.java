package com.spingbootinit.controller;

import com.spingbootinit.config.AsyncConfig;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.dto.bailian.BailianAssistChatRequest;
import com.spingbootinit.model.dto.bailian.ProblemStructureRequest;
import com.spingbootinit.model.vo.bailian.ProblemStructureVO;
import com.spingbootinit.common.result.Result;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.IsAdminRoleString;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.service.UserService;
import com.spingbootinit.service.impl.BailianAssistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/bailian_assist")
@Tag(name = "BailianAssist", description = "做题页 · 百炼学习助手")
public class BailianAssistController {

    private static final long SSE_TIMEOUT_MS = 30L * 60 * 1000;

    @Resource
    private BailianAssistService bailianAssistService;
    @Resource private UserService userService;
    @Resource private IsAdminRoleString isAdminRole;

    @Resource
    @Qualifier(AsyncConfig.BAILIAN_ASSIST_EXECUTOR)
    private Executor asyncExecutor;

    /**
     * 学习助手对话：仅支持 SSE（text/event-stream）。
     * 事件名 {@code delta} / {@code error} / {@code done}；
     * delta 的 data 为 JSON {@code {"t":"片段"}}，error 为 {@code {"notice":"说明"}}。
     */
    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "学习助手对话（SSE 流式）")
    public SseEmitter chat(HttpServletResponse response, @Valid @RequestBody BailianAssistChatRequest request) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        Long uid = UserContext.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        asyncExecutor.execute(() -> bailianAssistService.streamChat(uid, request, emitter));
        return emitter;
    }

    @PostMapping(value = "/structure-problem", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "将 OCR 文本结构化为传统 OJ 题目")
    public Result<ProblemStructureVO> structureProblem(@Valid @RequestBody ProblemStructureRequest request) {
        Long uid = UserContext.getCurrentUserId();
        User user = uid == null ? null : userService.getById(uid);
        if (user == null) throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        if (!isAdminRole.isAdminRoleString(user.getUserRole())) throw new BusinessException(ResultCode.NO_AUTH_ERROR);
        return Result.success(bailianAssistService.structureProblem(uid, request));
    }
}
