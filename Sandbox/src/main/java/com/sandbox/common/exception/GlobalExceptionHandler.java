package com.sandbox.common.exception;

import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.model.JudgeInfo;
import com.sandbox.sandbox.template.SandboxError;
import com.sandbox.sandbox.template.SandboxException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SandboxException.class)
    @ResponseStatus(HttpStatus.OK)
    public ExecuteCodeResponse handleSandboxException(SandboxException e) {
        return buildResponse(e.error, e.userMessage, e.detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    @ResponseStatus(HttpStatus.OK)
    public ExecuteCodeResponse handleBadRequest(Exception e) {
        return buildResponse(SandboxError.BAD_REQUEST, "请求参数错误", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public ExecuteCodeResponse handleIllegalArgumentException(IllegalArgumentException e) {
        return buildResponse(SandboxError.BAD_REQUEST, "请求参数错误", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ExecuteCodeResponse handleException(Exception e) {
        return buildResponse(SandboxError.SYSTEM_ERROR, "系统内部异常", e.getMessage());
    }

    private ExecuteCodeResponse buildResponse(SandboxError error, String message, String detail) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setCode(error.code);
        response.setRequestId(UUID.randomUUID().toString());
        response.setResultType(error.resultType);
        response.setStatus(error.status);
        response.setMessage(message);
        response.setOutputList(Collections.emptyList());

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(detail == null ? "" : detail);
        judgeInfo.setTime(0L);
        judgeInfo.setMemory(0L);
        response.setJudgeInfo(judgeInfo);
        return response;
    }
}
