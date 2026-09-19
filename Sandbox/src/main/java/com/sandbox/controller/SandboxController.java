package com.sandbox.controller;

import com.sandbox.annotation.RateLimit;
import com.sandbox.config.SandboxProperties;
import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.sandbox.template.SandboxError;
import com.sandbox.sandbox.template.SandboxException;
import com.sandbox.service.CodeSandboxFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.sandbox.config.DockerHelper;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SandboxController {

    private final CodeSandboxFactory codeSandboxFactory;
    private final SandboxProperties sandboxProperties;

    public SandboxController(CodeSandboxFactory codeSandboxFactory, SandboxProperties sandboxProperties) {
        this.codeSandboxFactory = codeSandboxFactory;
        this.sandboxProperties = sandboxProperties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (var client = DockerHelper.client()) {
            var version = client.versionCmd().exec();
            result.put("status", "ok");
            result.put("docker", version.getVersion());
            result.put("pythonImage", sandboxProperties.getDocker().getPythonImage());
        } catch (Exception e) {
            result.put("status", "degraded");
            result.put("docker", "unavailable");
        }
        return result;
    }

    @RateLimit(key = "sandbox", windowSeconds = 60, limit = 10)
    @PostMapping("/sandbox")
    public ExecuteCodeResponse test(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestBody ExecuteCodeRequest executeCodeRequest) throws InterruptedException {
        String tokenKey = request.getHeader("auth");
        if (!sandboxProperties.getAuth().getKey().equals(tokenKey)) {
            throw new SandboxException(SandboxError.BAD_REQUEST, "无权限访问", "invalid auth header");
        }
        return codeSandboxFactory.doExecuteCode(executeCodeRequest);
    }
}
