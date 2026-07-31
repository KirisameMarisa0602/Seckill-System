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
    public static final String DELAY_QUEUE = "seckill.delay.queue";
    public static final String DELAY_EXCHANGE = "seckill.delay.exchange";
    public static final String DELAY_ROUTING_KEY = "seckill.delay.routing.key";
    public static final String DEAD_LETTER_QUEUE = "seckill.dlq.queue";
    public static final String DEAD_LETTER_EXCHANGE = "seckill.dlx.exchange";
    public static final String DEAD_LETTER_ROUTING_KEY = "seckill.dlx.routing.key";
    public static final String ERROR_DEAD_LETTER_QUEUE = "seckill.error.dlq.queue";
    public static final String ERROR_DEAD_LETTER_EXCHANGE = "seckill.error.dlx.exchange";
    public static final String ERROR_DEAD_LETTER_ROUTING_KEY = "seckill.error.dlx.routing.key";

    @Bean
    public Queue seckillQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ERROR_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", ERROR_DEAD_LETTER_ROUTING_KEY);
        return new Queue(SECKILL_QUEUE, true, false, false, args);
    }

    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(ROUTING_KEY);
    }

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

    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
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
    public DirectExchange errorDeadLetterExchange() {
        return new DirectExchange(ERROR_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue errorDeadLetterQueue() {
        return new Queue(ERROR_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding errorDeadLetterBinding() {
        return BindingBuilder.bind(errorDeadLetterQueue()).to(errorDeadLetterExchange()).with(ERROR_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}