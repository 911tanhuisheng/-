package com.spingbootinit.judo.codesandbox;

import com.spingbootinit.judo.codesandbox.model.ExecuteCodeRequest;
import com.spingbootinit.judo.codesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodeSandboxProxy implements CodeSandbox{

    private final CodeSandbox codeSandbox;


    public CodeSandboxProxy(CodeSandbox codeSandbox) {
        this.codeSandbox = codeSandbox;
    }
    /**
     * 调用沙箱进行代码执行 日记的记录
     * @param executeCodeRequest
     * @return
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        int caseCount = executeCodeRequest == null || executeCodeRequest.getInputList() == null
                ? 0 : executeCodeRequest.getInputList().size();
        int imageCount = executeCodeRequest == null || executeCodeRequest.getImageBase64List() == null
                ? 0 : executeCodeRequest.getImageBase64List().size();
        log.info("代码沙箱调用开始 language={}, cases={}, images={}",
                executeCodeRequest == null ? null : executeCodeRequest.getLanguage(), caseCount, imageCount);
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        log.info("代码沙箱响应信息：" + executeCodeResponse.toString());
        return executeCodeResponse;
    }
}
