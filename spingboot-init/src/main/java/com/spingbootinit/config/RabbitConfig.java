package com.spingbootinit.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /**
     * 创建队列
     *
     * @return
     */
    @Bean
    public Queue queue() {

        /*
          1.队列名称
          2.是否持久化
         */
        return new Queue("hello.queue", true);
    }
}
