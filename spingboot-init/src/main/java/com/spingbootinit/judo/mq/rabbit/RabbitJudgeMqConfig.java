package com.spingbootinit.judo.mq.rabbit;

import com.spingbootinit.judo.mq.JudgeMqProperties;
import com.spingbootinit.judo.mq.JudgeMqProperties.Mode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模式 {@code rabbitmq} 时的 Spring 配置：交换机、队列、JSON 序列化。
 * <p>
 * 使用 rabbitmq 时请注释掉 application.yml 里对 RabbitAutoConfiguration 的 exclude。
 */
@Configuration
// 装配条件:只有当这个配置jude.mq.mode=rabbitmq时，才会启用此配置
@ConditionalOnProperty(name = "judge.mq.mode", havingValue = Mode.RABBITMQ) // 激活条件判断,是否为rabbitmq模式
@ImportAutoConfiguration(RabbitAutoConfiguration.class) // 导入RabbitAutoConfiguration
@EnableRabbit // 启用RabbitMQ
public class RabbitJudgeMqConfig {

    /**
     * 创建 JSON 消息转换器，用于序列化/反序列化消息。
     */
    @Bean
    public MessageConverter judgeRabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建 RabbitTemplate，用于发送消息。
     * connectionFactory: RabbitMQ 连接工厂
     * judgeRabbitMessageConverter: 消息转换器,这样消息就是json格式
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter judgeRabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(judgeRabbitMessageConverter);
        return template;
    }   


    /**
     * 创建 RabbitMQ 监听容器工厂，用于监听队列。
     * connectionFactory: RabbitMQ 连接工厂
     * judgeRabbitMessageConverter: 消息转换器
     */
    @Bean(name = "judgeRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory judgeRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter judgeRabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(judgeRabbitMessageConverter);
        return factory;
    }

    /**
     * 创建 RabbitMQ 交换机、队列、绑定关系。
     * DirectExchange: 直连交换机
     * true: 持久化交换机
     * false: 不持久化交换机
     */
    @Bean
    public DirectExchange judgeExchange(JudgeMqProperties properties) {
        return new DirectExchange(properties.getRabbit().getExchange(), true, false);
    }

    /**
     * 创建 RabbitMQ 队列。
     * true: 持久化队列
     * false: 不持久化队列
     */
    @Bean
    public Queue judgeQueue(JudgeMqProperties properties) {
        return new Queue(properties.getRabbit().getQueue(), true);
    }

    /**
     * 创建 RabbitMQ 绑定关系。
     * Queue: 队列
     * Exchange: 交换机
     * routingKey: 路由键
     */
    @Bean
    public Binding judgeBinding(Queue judgeQueue, DirectExchange judgeExchange, JudgeMqProperties properties) {
        return BindingBuilder.bind(judgeQueue)
                .to(judgeExchange)
                .with(properties.getRabbit().getRoutingKey());
    }
}
