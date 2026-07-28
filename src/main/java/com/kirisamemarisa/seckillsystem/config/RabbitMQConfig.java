package com.kirisamemarisa.seckillsystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 常量定义，避免魔法值
    public static final String SECKILL_QUEUE = "seckillQueue";
    public static final String SECKILL_EXCHANGE = "seckillExchange";
    public static final String ROUTING_KEY = "seckill.#";

    /**
     * 1. 声明普通的秒杀队列
     */
    @Bean
    public Queue seckillQueue() {
        // name, durable(持久化), exclusive(排他), autoDelete(自动删除)
        return new Queue(SECKILL_QUEUE, true);
    }

    /**
     * 2. 声明主题交换机 (Topic Exchange，支持通配符路由，大厂最爱)
     */
    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE);
    }

    /**
     * 3. 将 队列 和 交换机 通过 RoutingKey 绑定起来
     */
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange())
                .with(ROUTING_KEY);
    }

    /**
     * 4. 关键：替换掉默认的 JDK 序列化，改用 JSON 序列化消息！
     * 这样你在 RabbitMQ 管理面板看到的才不是乱码，而是清晰的 JSON 字符串，极大方便排错。
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}