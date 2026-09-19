package com.sandbox.dockertest;

import cn.hutool.core.io.resource.ResourceUtil;
import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.service.impl.java.DockerJavaCodeSandbox;
import com.sandbox.service.impl.python.DockerPythonCodeSandbox;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 独立 demo：不改现有控制器，直接 main 运行验证 Docker 沙箱。
 */
public class DockerPythonCodeSandboxDemo {
    public static void main(String[] args) {
        DockerPythonCodeSandbox sandbox = new DockerPythonCodeSandbox();

        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.py", StandardCharsets.UTF_8);

        ExecuteCodeRequest req = ExecuteCodeRequest.builder()
                .language("python")
                .code(code)
                .inputList(Arrays.asList("1 2", "10 20"))
                .build();

        ExecuteCodeResponse resp = sandbox.executeCode(req);
        System.out.println(resp);
    }
}

