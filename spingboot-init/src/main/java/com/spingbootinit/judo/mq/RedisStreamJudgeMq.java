package com.spingbootinit.judo.mq;

import com.spingbootinit.judo.JudgeService;
import com.spingbootinit.judo.mq.JudgeMqProperties.Mode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * 模式 {@code redis-stream}：一个类搞定「入队 + 监听」。
 * <ul>
 *   <li>入队：{@link #publish(Long)} → Redis XADD</li>
 *   <li>消费：启动时监听 Stream → {@link JudgeService#doJudge(long)}</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "judge.mq.mode", havingValue = Mode.REDIS_STREAM)
public class RedisStreamJudgeMq implements InitializingBean, DisposableBean {

    private static final String FIELD_SUBMIT_ID = "submitId";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisConnectionFactory redisConnectionFactory;
    @Resource
    private JudgeMqProperties properties;
    @Resource
    @Lazy
    private JudgeService judgeService;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    // -------------------- 生产者：提交后调用 --------------------

    public void publish(Long submitId) {
        if (submitId == null) {
            return;
        }
        String key = properties.getStreamKey();
        Map<String, String> body = Collections.singletonMap(FIELD_SUBMIT_ID, String.valueOf(submitId));
        var recordId = stringRedisTemplate.opsForStream().add(StreamRecords.string(body).withStreamKey(key));
        log.info("[Redis Stream] 入队 key={}, submitId={}, id={}", key, submitId, recordId);                        
    }

    // -------------------- 消费者：应用启动后自动运行 --------------------

    @Override
    public void afterPropertiesSet() {
        createGroupIfAbsent();

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(2))
                .build();
        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        Consumer consumer = Consumer.from(properties.getGroup(), properties.getConsumer());
        StreamOffset<String> offset = StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed());
        container.receive(consumer, offset, this::handleMessage);
        container.start();

        log.info("[Redis Stream] 监听已启动 key={}, group={}", properties.getStreamKey(), properties.getGroup());
    }

    @Override
    public void destroy() {
        if (container != null) {
            container.stop();
        }
    }

    private void createGroupIfAbsent() {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(properties.getStreamKey(), ReadOffset.from("0-0"), properties.getGroup());
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            if (!StringUtils.contains(msg, "BUSYGROUP")) {
                throw e;
            }
        }
    }

    private void handleMessage(MapRecord<String, String, String> message) {
        String raw = message.getValue().get(FIELD_SUBMIT_ID);
        if (!StringUtils.isNumeric(raw)) {
            ack(message);
            return;
        }
        long submitId = Long.parseLong(raw);
        try {
            judgeService.doJudge(submitId);
            ack(message);
        } catch (Exception e) {
            log.error("[Redis Stream] 判题失败 submitId={}", submitId, e);
        }
    }

    private void ack(MapRecord<String, String, String> message) {
        stringRedisTemplate.opsForStream()
                .acknowledge(properties.getStreamKey(), properties.getGroup(), message.getId());
    }
}
