package com.spingbootinit.judo.mq.rabbit;

import com.spingbootinit.judo.mq.JudgeMqProperties;
import com.spingbootinit.judo.mq.JudgeMqProperties.Mode;
import com.spingbootinit.judo.mq.JudgeTaskTrigger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 模式 {@code rabbitmq}：发送 JSON 数字（submitId）到交换机。 生产者
 */
@Slf4j
@Component
// 当配置 judge.mq.mode=rabbitmq 时候，才加载
@ConditionalOnProperty(name = "judge.mq.mode", havingValue = Mode.RABBITMQ)
public class RabbitJudgeTaskTrigger implements JudgeTaskTrigger {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private JudgeMqProperties properties;

    @Override
    public String type() {
        return Mode.RABBITMQ;
    }

    @Override
    public void enqueue(Long submitId) {
        if (submitId == null) {
            return;
        }
        // 发送 JSON 数字（submitId）到交换机
        JudgeMqProperties.Rabbit rabbit = properties.getRabbit();
        rabbitTemplate.convertAndSend(rabbit.getExchange(), rabbit.getRoutingKey(), submitId);
        log.info("[RabbitMQ] 入队 exchange={}, key={}, submitId={}",
                rabbit.getExchange(), rabbit.getRoutingKey(), submitId);
    }
}
