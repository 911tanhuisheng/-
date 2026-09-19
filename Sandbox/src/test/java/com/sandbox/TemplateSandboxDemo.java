package com.sandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.service.impl.java.NativeJavaTemplateSandbox;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 本机直接跑的演示入口（不依赖 Spring）
 */
public class TemplateSandboxDemo {
    public static void main(String[] args) {
        NativeJavaTemplateSandbox sandbox = new NativeJavaTemplateSandbox();

        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);

        ExecuteCodeRequest req = ExecuteCodeRequest.builder()
                .language("java")
                .code(code)
                .inputList(List.of("11","12"))
                .build();

        ExecuteCodeResponse resp = sandbox.executeCode(req);
        System.out.println(resp);
    }
}

