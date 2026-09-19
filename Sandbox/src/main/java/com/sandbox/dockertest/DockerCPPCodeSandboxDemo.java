package com.sandbox.dockertest;

import cn.hutool.core.io.resource.ResourceUtil;
import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.service.CodeSandboxFactory;
import com.sandbox.service.impl.cpp.DockerCppCodeSandbox;
import com.sandbox.service.impl.java.DockerJavaCodeSandbox;
import jakarta.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 独立 demo：不改现有控制器，直接 main 运行验证 Docker 沙箱。
 */
public class DockerCPPCodeSandboxDemo {
    public static void main(String[] args) {
        DockerCppCodeSandbox sandbox = new DockerCppCodeSandbox();

        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.cpp", StandardCharsets.UTF_8);

        ExecuteCodeRequest req = ExecuteCodeRequest.builder()
                .language("cpp")
                .code(code)
                .inputList(Arrays.asList("1 2", "10 20"))
                .build();

        ExecuteCodeResponse resp = sandbox.executeCode(req);
        System.out.println(resp);
    }
}

