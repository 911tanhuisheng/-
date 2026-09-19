package com.sandbox;

import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.sandbox.template.AbstractTemplateCodeSandbox;
import com.sandbox.sandbox.template.SandboxContext;
import com.sandbox.sandbox.template.SandboxError;
import com.sandbox.sandbox.template.SandboxException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SandboxResponseProtocolTest {

    @Test
    void compileErrorKeepsMachineTypeAndReadableJudgeStatus() {
        AbstractTemplateCodeSandbox sandbox = new AbstractTemplateCodeSandbox() {
            @Override protected void securityCheck(SandboxContext ctx) { }
            @Override protected void saveCodeToFile(SandboxContext ctx) { }
            @Override protected void compile(SandboxContext ctx) {
                throw new SandboxException(SandboxError.COMPILE_ERROR, "编译失败", "line 1: invalid syntax");
            }
            @Override protected void cleanup(SandboxContext ctx) { }
        };

        ExecuteCodeResponse response = sandbox.executeCode(ExecuteCodeRequest.builder()
                .language("java").code("broken").inputList(List.of("1")).build());

        assertEquals("COMPILE_ERROR", response.getResultType());
        assertEquals("Compile Error", response.getJudgeInfo().getMessage());
        assertEquals("line 1: invalid syntax", response.getJudgeInfo().getDetail());
        assertFalse(response.getRequestId().isBlank());
    }
}
