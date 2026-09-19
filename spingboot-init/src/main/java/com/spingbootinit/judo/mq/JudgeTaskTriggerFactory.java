package com.spingbootinit.judo.mq;

import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工厂：根据 {@code judge.mq.mode} 从 Spring 容器里已注册的策略中选一个执行。
 * <p>
 * 业务层只依赖本类，不感知底层是线程池还是 MQ。
 */
@Slf4j
@Component
public class JudgeTaskTriggerFactory {

    private final Map<String, JudgeTaskTrigger> triggers;
    private final JudgeMqProperties properties;

    /**
     * Stream() 把List变成流水线
     * collect() 把流水线变成一个容器里
     * Collectors.toMap() 把流水线变成一个Map
     * Collectors.toMap() 第一个参数是键，第二个参数是值，第三个参数是冲突处理逻辑
     * Function.identity() 返回参数本身
     */
    public JudgeTaskTriggerFactory(List<JudgeTaskTrigger> triggerList, JudgeMqProperties properties) {
        this.triggers = triggerList.stream()
                .collect(Collectors.toMap(JudgeTaskTrigger::type, Function.identity(), (a, b) -> a));
        this.properties = properties;
    }

    @PostConstruct
    void logMode() {
        log.info("[判题分发] mode={}，可用策略={}", properties.getMode(), triggers.keySet());
    }

    /** 提交代码后调用：把 submitId 交给当前模式处理 */
    public void enqueue(Long submitId) {
        JudgeTaskTrigger trigger = triggers.get(properties.getMode());
        System.out.println("trigger:" + trigger);
        if (trigger == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(),
                    "未知 judge.mq.mode=" + properties.getMode()
                            + "，可选: async, redis-stream, rabbitmq");
        }
        trigger.enqueue(submitId);
    }
}
