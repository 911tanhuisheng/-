package com.spingbootinit.judo.codesandbox.impl;


import com.spingbootinit.judo.codesandbox.CodeSandbox;
import com.spingbootinit.judo.codesandbox.model.ExecuteCodeRequest;
import com.spingbootinit.judo.codesandbox.model.ExecuteCodeResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 远程代码沙箱（实际调用接口的沙箱）
 */
@Service
@Slf4j
public class RemoteCodeSandbox implements CodeSandbox {

    @Resource
    private ExampleCodeSandbox exampleCodeSandbox;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("使用 remote 沙箱执行代码");
        return exampleCodeSandbox.executeCode(executeCodeRequest);
    }
}
