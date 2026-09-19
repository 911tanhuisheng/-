package com.spingbootinit.judo.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对应 application.yml 中 {@code judge.mq.*}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "judge.mq")
public class JudgeMqProperties {

    /** 可选值见 {@link Mode} */
    private String mode = Mode.ASYNC;

    /** Redis Stream 键名 */
    private String streamKey = "judge:stream";
    private String group = "judge-group";
    private String consumer = "judge-consumer-1";

    private final Rabbit rabbit = new Rabbit();

    /** 三种模式的配置字符串，与 yml 中 judge.mq.mode 保持一致 */
    public static final class Mode {
        public static final String ASYNC = "async";
        public static final String REDIS_STREAM = "redis-stream";
        public static final String RABBITMQ = "rabbitmq";

        private Mode() {
        }
    }

    @Data
    public static class Rabbit {
        private String exchange = "judge.exchange";
        private String queue = "judge.queue";
        private String routingKey = "judge.submit";
    }
}
