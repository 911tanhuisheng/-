package com.sandbox.sandbox.template;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.sandbox.config.SandboxProperties;
import com.sandbox.model.ExecuteCodeRequest;
import com.sandbox.model.ExecuteCodeResponse;
import com.sandbox.model.ExecuteMessage;
import com.sandbox.model.JudgeInfo;
import com.sandbox.service.CodeSandbox;
import com.sandbox.utils.ProcessUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 模板方法模式：把 OJ 沙箱核心流程固定下来
 * <p>
 * 1.保存代码 -> 2.编译 -> 3.执行 -> 4.整理输出 -> 5.清理 -> 6.错误处理
 * <p>
 * 你后续做 Docker 沙箱时，只需要复用这个模板，替换 compile/run/cleanup 的实现即可。
 */
public abstract class AbstractTemplateCodeSandbox implements CodeSandbox {

    private static final String MAIN_JAVA = "Main.java";

    @Autowired
    protected SandboxProperties sandboxProperties;

    private static ProcessBuilder buildRunPb(File workDir, String classpath, String stdin) {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-Xmx256m");
        cmd.add("-Xss256k");
        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add("Main");

        if (StrUtil.isNotBlank(stdin)) {
            for (String token : stdin.trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    cmd.add(token);
                }
            }
        }

        return new ProcessBuilder(cmd).directory(workDir);
    }

    protected static String firstDetail(ExecuteMessage m) {
        if (m == null) return "";
        if (StrUtil.isNotBlank(m.getErrorMessage())) return m.getErrorMessage().trim();
        return StrUtil.nullToEmpty(m.getMessage()).trim();
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        SandboxContext ctx = new SandboxContext(request);
        try {
            validateRequest(ctx);
            securityCheck(ctx);
            saveCodeToFile(ctx);
            compile(ctx);
            runAll(ctx);
            return buildSuccessResponse(ctx);
        } catch (SandboxException e) {
            return buildFailResponse(ctx, e);
        } catch (Exception e) {
            return buildFailResponse(ctx, new SandboxException(SandboxError.SYSTEM_ERROR, "沙箱内部异常", e.getMessage()));
        } finally {
            try {
                cleanup(ctx);
            } catch (Exception ignore) {
            }
        }
    }

    protected void saveCodeToFile(SandboxContext ctx) {
        String base = sandboxProperties.getTmpCodeDir();
        if (!FileUtil.exist(base)) {
            FileUtil.mkdir(base);
        }

        String work = base + File.separator + UUID.randomUUID();
        FileUtil.mkdir(work);

        ctx.workDir = new File(work);
        ctx.sourceFile = FileUtil.writeString(ctx.request.getCode(), new File(ctx.workDir, MAIN_JAVA), StandardCharsets.UTF_8);
        if (ctx.sourceFile == null || !ctx.sourceFile.exists()) {
            throw new SandboxException(SandboxError.SYSTEM_ERROR, "源码写入失败", "write source file failed");
        }
    }

    protected void compile(SandboxContext ctx) {
        ProcessBuilder pb = new ProcessBuilder(
                "javac", "-encoding", "UTF-8", MAIN_JAVA
        ).directory(ctx.workDir);

        ExecuteMessage m = ProcessUtils.runProcess(pb, "", sandboxProperties.getExecution().getLocalCompileTimeoutMs());
        if (m.getExitValue() == null || m.getExitValue() != 0) {
            throw new SandboxException(SandboxError.COMPILE_ERROR, "编译失败", firstDetail(m));
        }
    }

    protected void runAll(SandboxContext ctx) {
        for (String input : ctx.inputList) {
            ProcessBuilder pb = buildRunPb(ctx.workDir, ctx.workDir.getAbsolutePath(), input);
            ExecuteMessage m = ProcessUtils.runProcess(
                    pb,
                    input == null ? "" : input,
                    sandboxProperties.getExecution().getLocalRunTimeoutMs()
            );

            if (m.getTime() != null) {
                ctx.maxTimeMs = Math.max(ctx.maxTimeMs, m.getTime());
            }

            if (m.getExitValue() == null || m.getExitValue() != 0) {
                boolean tle = m.getExitValue() != null && m.getExitValue() == -1;
                SandboxError err = tle ? SandboxError.TIME_LIMIT : SandboxError.RUNTIME_ERROR;
                String msg = tle ? "时间限制超出" : "运行失败";
                throw new SandboxException(err, msg, firstDetail(m));
            }

            ctx.outputList.add(StrUtil.nullToEmpty(m.getMessage()).trim());
        }
    }

    protected void cleanup(SandboxContext ctx) {
        if (ctx.workDir != null) {
            FileUtil.del(ctx.workDir);
        }
    }

    protected void validateRequest(SandboxContext ctx) {
        if (ctx.request == null || ctx.request.getCode() == null || ctx.request.getCode().isBlank()) {
            throw new SandboxException(SandboxError.BAD_REQUEST, "代码为空", "code is blank");
        }
        if (ctx.request.getLanguage() == null || !"java".equalsIgnoreCase(ctx.request.getLanguage())) {
            throw new SandboxException(SandboxError.BAD_REQUEST, "仅支持 Java", "unsupported language");
        }
        List<String> inputList = ctx.request.getInputList();
        ctx.inputList = (inputList == null || inputList.isEmpty()) ? Collections.singletonList("") : inputList;
    }

    protected void securityCheck(SandboxContext ctx) {
        SecurityPolicy securityPolicy = new SecurityPolicy(
                sandboxProperties.getSecurity().getMaxCodeLength(),
                sandboxProperties.getSecurity().getMaxCaseCount()
        );
        securityPolicy.check(ctx);
    }

    protected ExecuteCodeResponse buildSuccessResponse(SandboxContext ctx) {
        ExecuteCodeResponse resp = new ExecuteCodeResponse();
        resp.setCode(SandboxError.SUCCESS.code);
        resp.setRequestId(UUID.randomUUID().toString());
        resp.setResultType(SandboxError.SUCCESS.resultType);
        resp.setStatus(SandboxError.SUCCESS.status);
        resp.setMessage("执行成功");
        resp.setOutputList(ctx.outputList);

        JudgeInfo ji = new JudgeInfo();
        ji.setMessage("Accepted");
        ji.setTime(ctx.maxTimeMs);
        ji.setMemory(0L);
        resp.setJudgeInfo(ji);
        return resp;
    }

    protected ExecuteCodeResponse buildFailResponse(SandboxContext ctx, SandboxException e) {
        ExecuteCodeResponse resp = new ExecuteCodeResponse();
        SandboxError error = e.error == null ? SandboxError.SYSTEM_ERROR : e.error;
        resp.setCode(error.code);
        resp.setRequestId(UUID.randomUUID().toString());
        resp.setResultType(error.resultType);
        resp.setStatus(error.status);
        resp.setMessage(e.userMessage);
        resp.setOutputList(ctx.outputList == null ? Collections.emptyList() : ctx.outputList);

        JudgeInfo ji = new JudgeInfo();
        ji.setMessage(toJudgeMessage(error));
        // 原始编译器/运行时信息单独保存，避免调用方无法识别标准状态。
        ji.setDetail(e.detail == null ? "" : e.detail);
        ji.setTime(ctx.maxTimeMs);
        ji.setMemory(0L);
        resp.setJudgeInfo(ji);
        return resp;
    }

    private static String toJudgeMessage(SandboxError error) {
        return switch (error) {
            case COMPILE_ERROR -> "Compile Error";
            case RUNTIME_ERROR -> "Runtime Error";
            case TIME_LIMIT -> "Time Limit Exceeded";
            case SECURITY_REJECT -> "Dangerous Operation";
            case SYSTEM_ERROR, BAD_REQUEST, RATE_LIMITED, SERVER_BUSY -> "System Error";
            default -> "System Error";
        };
    }
}
