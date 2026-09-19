package com.spingbootinit.judo.mq;

import com.spingbootinit.judo.mq.JudgeMqProperties.Mode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 模式 {@code async}：把判题丢进本机线程池，不经过 Redis/Rabbit。
 * <p>
 * 适合：本地开发、单机部署。
 */
@Component
public class AsyncJudgeTaskTrigger implements JudgeTaskTrigger {

    @Resource
    private JudgeAsyncExecutor judgeAsyncExecutor;

    @Override
    public String type() {
        return Mode.ASYNC;
    }

    @Override
    public void enqueue(Long submitId) {
        judgeAsyncExecutor.submitJudgeAsync(submitId);
    }
}
