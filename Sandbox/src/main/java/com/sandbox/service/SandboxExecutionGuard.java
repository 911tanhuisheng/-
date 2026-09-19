package com.sandbox.service;

import com.sandbox.config.SandboxProperties;
import com.sandbox.sandbox.template.SandboxError;
import com.sandbox.sandbox.template.SandboxException;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SandboxExecutionGuard {

    private final SandboxProperties sandboxProperties;
    private final AtomicInteger configuredMaxConcurrent = new AtomicInteger(0);
    private volatile Semaphore semaphore;

    public SandboxExecutionGuard(SandboxProperties sandboxProperties) {
        this.sandboxProperties = sandboxProperties;
        rebuildSemaphoreIfNeeded();
    }

    public void acquire() {
        rebuildSemaphoreIfNeeded();
        long timeoutMs = sandboxProperties.getExecution().getAcquireTimeoutMs();
        boolean acquired;
        try {
            acquired = timeoutMs <= 0 ? semaphore.tryAcquire() : semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SandboxException(
                    SandboxError.SERVER_BUSY,
                    "服务器繁忙，请稍后再试",
                    "sandbox acquire interrupted"
            );
        }
        if (!acquired) {
            throw new SandboxException(
                    SandboxError.SERVER_BUSY,
                    "服务器繁忙，请稍后再试",
                    "too many concurrent sandbox executions"
            );
        }
    }

    public void release() {
        if (semaphore != null) {
            semaphore.release();
        }
    }

    public int availablePermits() {
        rebuildSemaphoreIfNeeded();
        return semaphore.availablePermits();
    }

    private synchronized void rebuildSemaphoreIfNeeded() {
        int maxConcurrent = Math.max(1, sandboxProperties.getExecution().getMaxConcurrent());
        if (semaphore == null || configuredMaxConcurrent.get() != maxConcurrent) {
            semaphore = new Semaphore(maxConcurrent, true);
            configuredMaxConcurrent.set(maxConcurrent);
        }
    }
}
