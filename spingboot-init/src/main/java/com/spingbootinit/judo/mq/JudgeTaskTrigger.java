package com.spingbootinit.judo.mq;

/**
 * 判题入队策略：每种实现对应一种 {@link JudgeMqProperties.Mode}。
 * <p>
 * 只做一件事 —— 收到 submitId 后，用对应方式触发后台 {@code doJudge}。
 */
public interface JudgeTaskTrigger {

    /** 与 judge.mq.mode 相同，例如 {@code async} */
    String type();

    void enqueue(Long submitId);
}
