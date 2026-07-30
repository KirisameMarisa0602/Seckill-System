package com.kirisamemarisa.seckillsystem.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    public static final String SECKILL_QUEUE = "seckillQueue";
    public static final String SECKILL_EXCHANGE = "seckillExchange";
    public static final String ROUTING_KEY = "seckill.#";

    // --- 新增：死信队列与延迟队列相关配置 ---
    public static final String DELAY_QUEUE = "seckill.delay.queue";
    public static final String DELAY_EXCHANGE = "seckill.delay.exchange";
    public static final String DELAY_ROUTING_KEY = "seckill.delay.routing.key";

    public static final String DEAD_LETTER_QUEUE = "seckill.dlq.queue";
    public static final String DEAD_LETTER_EXCHANGE = "seckill.dlx.exchange";
    public static final String DEAD_LETTER_ROUTING_KEY = "seckill.dlx.routing.key";

    @Bean
    public Queue seckillQueue() {
        return new Queue(SECKILL_QUEUE, true);
    }
    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE);
    }
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(ROUTING_KEY);
    }

    // 1. 死信交换机与死信队列（用来接收超时的死亡消息）
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_ROUTING_KEY);
    }

    // 2. 延迟队列（无消费者，消息待够时间后自动转给死信交换机）
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 声明该队列的死信交换机
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        // 声明该队列死信消息的路由键
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        // 声明队列消息的过期时间，这里设为 1分钟 (方便测试，生产换成 30分钟即 1800000)
        args.put("x-message-ttl", 60000);
        return new Queue(DELAY_QUEUE, true, false, false, args);
    }
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(DELAY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}