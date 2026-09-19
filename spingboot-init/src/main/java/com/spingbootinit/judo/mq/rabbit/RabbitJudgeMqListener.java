package com.spingbootinit.judo.mq.rabbit;

import com.spingbootinit.judo.JudgeService;
import com.spingbootinit.judo.mq.JudgeMqProperties.Mode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 模式 {@code rabbitmq}：监听队列，收到 submitId 后判题。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "judge.mq.mode", havingValue = Mode.RABBITMQ)
public class RabbitJudgeMqListener {

    @Resource
    @Lazy
    private JudgeService judgeService;

    @RabbitListener(
            queues = "${judge.mq.rabbit.queue}",
            containerFactory = "judgeRabbitListenerContainerFactory"
    )
    public void onMessage(Long submitId) {
        if (submitId == null) {
            return;
        }
        try {
            log.debug("[RabbitMQ] 开始判题 submitId={}", submitId);
            judgeService.doJudge(submitId);
        } catch (Exception e) {
            log.error("[RabbitMQ] 判题失败 submitId={}", submitId, e);
            throw e;
        }
    }
}
