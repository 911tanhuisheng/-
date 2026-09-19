package com.spingbootinit.judo.mq;

import com.spingbootinit.judo.mq.JudgeMqProperties.Mode;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 模式 {@code redis-stream}：委托 {@link RedisStreamJudgeMq#publish} 入队。 */
@Component
@ConditionalOnProperty(name = "judge.mq.mode", havingValue = Mode.REDIS_STREAM)
public class RedisStreamJudgeTaskTrigger implements JudgeTaskTrigger {

    @Resource
    private RedisStreamJudgeMq redisStreamJudgeMq;

    @Override
    public String type() {
        return Mode.REDIS_STREAM;
    }

    @Override
    public void enqueue(Long submitId) {
        redisStreamJudgeMq.publish(submitId);
    }
}
