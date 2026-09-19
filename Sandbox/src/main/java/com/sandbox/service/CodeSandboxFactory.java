package com.sandbox.service;


import com.sandbox.constant.LanguageConstant;
import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.sandbox.template.SandboxError;
import com.sandbox.sandbox.template.SandboxException;
import com.sandbox.service.impl.cpp.DockerCppCodeSandbox;
import com.sandbox.service.impl.java.DockerJavaCodeSandbox;
import com.sandbox.service.impl.java.NativeJavaTemplateSandbox;
import com.sandbox.service.impl.python.DockerPythonCodeSandbox;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
public class CodeSandboxFactory {

    private final DockerJavaCodeSandbox dockerJavaCodeSandbox;
    private final DockerPythonCodeSandbox dockerPythonCodeSandbox;
    private final DockerCppCodeSandbox dockerCppCodeSandbox;
    private final SandboxExecutionGuard executionGuard;

    public ExecuteCodeResponse doExecuteCode(ExecuteCodeRequest executeCodeRequest) throws InterruptedException {
        if (executeCodeRequest == null) {
            throw new SandboxException(SandboxError.BAD_REQUEST, "请求体为空", "request body is null");
        }
        String language = executeCodeRequest.getLanguage();
        if (language == null || language.isBlank()) {
            throw new SandboxException(SandboxError.BAD_REQUEST, "编程语言为空", "language is blank");
        }

        CodeSandbox codeSandbox = switch (executeCodeRequest.getLanguage()) {
            case LanguageConstant.JAVA -> dockerJavaCodeSandbox;
            case LanguageConstant.PYTHON -> dockerPythonCodeSandbox;
            case LanguageConstant.CPP -> dockerCppCodeSandbox;
            default -> throw new IllegalArgumentException("不支持的语言: " + executeCodeRequest.getLanguage());
        };

        executionGuard.acquire();
        try {
            return codeSandbox.executeCode(executeCodeRequest);
        } finally {
            executionGuard.release();
        }
    }
}
