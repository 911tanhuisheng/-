package com.sandbox.service.impl.java;

import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.sandbox.template.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 原生 Java 沙箱：严格按模板流程实现，可直接跑通。
 * 后续做 Docker 沙箱：复用模板类，仅替换 compile/run/cleanup。
 */
@Slf4j
@Service
public class NativeJavaTemplateSandbox extends AbstractTemplateCodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        return super.executeCode(request);
    }
}
