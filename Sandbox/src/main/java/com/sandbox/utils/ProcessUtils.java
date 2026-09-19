package com.sandbox.utils;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import com.sandbox.model.ExecuteMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 进程工具类（OJ：避免先 waitFor 再读流导致死锁；支持 stdin 与超时）
 */
public class ProcessUtils {

    private ProcessUtils() {
    }

    /**
     * 执行进程：先启动线程消费 stdout/stderr，再写 stdin 并关闭，最后 waitFor（超时强杀）。
     * 适用于：javac、java 等。
     *
     * @param stdin 可为 null 或 ""，会关闭子进程 stdin
     */
    public static ExecuteMessage runProcess(java.lang.ProcessBuilder pb, String stdin, long timeoutMs) {
        ExecuteMessage msg = new ExecuteMessage();
        pb.redirectErrorStream(false);
        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            msg.setExitValue(-1);
            msg.setErrorMessage("启动进程失败: " + e.getMessage());
            msg.setTime(0L);
            msg.setMemory(0L);
            return msg;
        }

        StopWatch sw = new StopWatch();
        sw.start();

        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        Thread tOut = new Thread(() -> drainStream(p.getInputStream(), out), "sandbox-stdout");
        Thread tErr = new Thread(() -> drainStream(p.getErrorStream(), err), "sandbox-stderr");
        tOut.start();
        tErr.start();

        try (OutputStream os = p.getOutputStream()) {
            if (StrUtil.isNotBlank(stdin)) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
            os.flush();
        } catch (IOException e) {
            msg.setExitValue(-1);
            msg.setErrorMessage("写入 stdin 失败: " + e.getMessage());
            p.destroyForcibly();
            joinQuietly(tOut, tErr);
            sw.stop();
            msg.setTime(sw.getLastTaskTimeMillis());
            msg.setMemory(0L);
            return msg;
        }

        boolean finished;
        try {
            finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
            joinQuietly(tOut, tErr);
            sw.stop();
            msg.setExitValue(-1);
            msg.setErrorMessage("线程被中断");
            msg.setTime(sw.getLastTaskTimeMillis());
            msg.setMemory(0L);
            return msg;
        }

        joinQuietly(tOut, tErr);
        sw.stop();
        msg.setTime(sw.getLastTaskTimeMillis());
        msg.setMemory(0L);
        msg.setMessage(out.toString());
        msg.setErrorMessage(err.toString());

        if (!finished) {
            p.destroyForcibly();
            msg.setExitValue(-1);
            if (StrUtil.isBlank(msg.getErrorMessage())) {
                msg.setErrorMessage("时间限制超出");
            } else {
                msg.setErrorMessage("时间限制超出\n" + msg.getErrorMessage());
            }
            return msg;
        }

        msg.setExitValue(p.exitValue());
        return msg;
    }

    private static void drainStream(InputStream in, StringBuilder sb) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        } catch (IOException ignored) {
            // 进程被 kill 时可能异常，忽略
        }
    }

    private static void joinQuietly(Thread a, Thread b) {
        try {
            a.join(10_000);
            b.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 兼容旧调用：无 stdin、无超时（仅适合输出很小的进程，如 javac）
     *
     * @deprecated 新代码请使用 {@link #runProcess(ProcessBuilder, String, long)}
     */
    @Deprecated
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        // 仍先起线程读流，再 waitFor，避免死锁
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            Thread tOut = new Thread(() -> drainStream(runProcess.getInputStream(), out));
            Thread tErr = new Thread(() -> drainStream(runProcess.getErrorStream(), err));
            tOut.start();
            tErr.start();
            runProcess.getOutputStream().close();

            int exitValue = runProcess.waitFor();
            joinQuietly(tOut, tErr);

            executeMessage.setExitValue(exitValue);
            executeMessage.setMessage(out.toString());
            executeMessage.setErrorMessage(err.toString());
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            executeMessage.setMemory(0L);

            if (exitValue == 0) {
                System.out.println(opName + "成功");
            } else {
                System.out.println(opName + "失败，错误码： " + exitValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 交互式：向 stdin 写内容后读 stdout（旧实现易死锁，建议改用 runProcess）
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        return runInteractProcessAndGetMessage(runProcess, args, 60_000L);
    }

    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args, long timeoutMs) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            Thread tOut = new Thread(() -> drainStream(runProcess.getInputStream(), out));
            Thread tErr = new Thread(() -> drainStream(runProcess.getErrorStream(), err));
            tOut.start();
            tErr.start();

            try (OutputStreamWriter outputStreamWriter =
                         new OutputStreamWriter(runProcess.getOutputStream(), StandardCharsets.UTF_8)) {
                String join = StrUtil.isBlank(args) ? "" : args + "\n";
                outputStreamWriter.write(join);
                outputStreamWriter.flush();
            }

            boolean finished = runProcess.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            joinQuietly(tOut, tErr);

            executeMessage.setMessage(out.toString());
            executeMessage.setErrorMessage(err.toString());
            executeMessage.setMemory(0L);
            if (!finished) {
                runProcess.destroyForcibly();
                executeMessage.setExitValue(-1);
                executeMessage.setErrorMessage("时间限制超出\n" + executeMessage.getErrorMessage());
            } else {
                executeMessage.setExitValue(runProcess.exitValue());
            }
        } catch (Exception e) {
            executeMessage.setExitValue(-1);
            executeMessage.setErrorMessage(e.getMessage());
        }
        return executeMessage;
    }
}
