package com.kirisamemarisa.seckillsystem.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 交换机、队列与绑定。Broker 上的拓扑在应用启动时声明。
 *
 * <p>两条死信路径不要混用：
 * <ul>
 *   <li>下单队列 Nack 不重回 → 错误死信，用于重试/回补 Redis 预留库存</li>
 *   <li>延迟队列 15 分钟 TTL 到期 → 关单死信，用于超时未支付</li>
 * </ul>
 * 补偿队列在支付成功后扣 {@code t_goods} 主库存。消息体用 JSON，便于跨语言排障。
 * 依赖中间件：RabbitMQ。无 {@code @Order}。
 */
@Configuration
public class RabbitMQConfig {
    /** 秒杀下单队列（持久化；失败进错误死信）。 */
    public static final String SECKILL_QUEUE = "seckillQueue";
    /** 秒杀下单 Topic 交换机。 */
    public static final String SECKILL_EXCHANGE = "seckillExchange";
    /** 发送下单消息时使用的 routing key。 */
    public static final String SEND_ROUTING_KEY = "seckill.message";
    /** 下单队列绑定通配 {@code seckill.#}。 */
    public static final String BINDING_ROUTING_KEY = "seckill.#";
    /** 延迟关单队列，消息 TTL 15 分钟后转入关单死信。 */
    public static final String DELAY_QUEUE = "seckill.delay.queue";
    /** 延迟关单交换机。 */
    public static final String DELAY_EXCHANGE = "seckill.delay.exchange";
    /** 延迟关单 routing key。 */
    public static final String DELAY_ROUTING_KEY = "seckill.delay.routing.key";
    /** 超时关单死信队列。 */
    public static final String DEAD_LETTER_QUEUE = "seckill.dlq.queue";
    /** 超时关单死信交换机。 */
    public static final String DEAD_LETTER_EXCHANGE = "seckill.dlx.exchange";
    /** 超时关单死信 routing key。 */
    public static final String DEAD_LETTER_ROUTING_KEY = "seckill.dlx.routing.key";
    /** 下单消费失败后的错误死信队列。 */
    public static final String ERROR_DEAD_LETTER_QUEUE = "seckill.error.dlq.queue";
    /** 错误死信交换机。 */
    public static final String ERROR_DEAD_LETTER_EXCHANGE = "seckill.error.dlx.exchange";
    /** 错误死信 routing key。 */
    public static final String ERROR_DEAD_LETTER_ROUTING_KEY = "seckill.error.dlx.routing.key";
    /** 支付成功后扣主库存的补偿队列。 */
    public static final String COMPENSATE_QUEUE = "seckill.compensate.queue";
    /** 主库存补偿交换机。 */
    public static final String COMPENSATE_EXCHANGE = "seckill.compensate.exchange";
    /** 主库存补偿 routing key。 */
    public static final String COMPENSATE_ROUTING_KEY = "seckill.compensate.routing.key";

    /**
     * 下单队列：durable，绑定错误死信交换机，消费 Nack 且 {@code requeue=false} 时转入。
     */
    @Bean
    public Queue seckillQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ERROR_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", ERROR_DEAD_LETTER_ROUTING_KEY);
        return new Queue(SECKILL_QUEUE, true, false, false, args);
    }

    /** 秒杀下单 Topic 交换机。 */
    @Bean
    public TopicExchange seckillExchange() { return new TopicExchange(SECKILL_EXCHANGE); }

    /** 将 {@code seckill.#} 绑定到下单队列。 */
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(BINDING_ROUTING_KEY);
    }

    /** 超时关单死信交换机（延迟队列 TTL 到期后路由到此）。 */
    @Bean
    public DirectExchange deadLetterExchange() { return new DirectExchange(DEAD_LETTER_EXCHANGE); }

    /** 超时关单死信队列。 */
    @Bean
    public Queue deadLetterQueue() { return new Queue(DEAD_LETTER_QUEUE, true); }

    /** 绑定超时关单死信队列。 */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * 延迟队列：不消费业务，只靠 {@code x-message-ttl=900000}（15 分钟）把消息「变成」关单死信。
     * 用 TTL+DLX 而不是插件延迟交换机，降低对 Broker 插件的依赖。
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        args.put("x-message-ttl", 900000);
        return new Queue(DELAY_QUEUE, true, false, false, args);
    }

    /** 延迟关单交换机。 */
    @Bean
    public DirectExchange delayExchange() { return new DirectExchange(DELAY_EXCHANGE); }

    /** 绑定延迟关单队列。 */
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(DELAY_ROUTING_KEY);
    }

    /** 下单失败错误死信交换机。 */
    @Bean
    public DirectExchange errorDeadLetterExchange() { return new DirectExchange(ERROR_DEAD_LETTER_EXCHANGE); }

    /** 错误死信队列。 */
    @Bean
    public Queue errorDeadLetterQueue() { return new Queue(ERROR_DEAD_LETTER_QUEUE, true); }

    /** 绑定错误死信队列。 */
    @Bean
    public Binding errorDeadLetterBinding() {
        return BindingBuilder.bind(errorDeadLetterQueue()).to(errorDeadLetterExchange()).with(ERROR_DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器，使 {@code SeckillMessage} 等以可读 JSON 进出队列，而不是 Java 原生序列化。
     */
    @Bean
    public MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    /** 主库存补偿交换机。 */
    @Bean
    public DirectExchange compensateExchange() {
        return new DirectExchange(COMPENSATE_EXCHANGE);
    }

    /** 主库存补偿队列。 */
    @Bean
    public Queue compensateQueue() {
        return new Queue(COMPENSATE_QUEUE, true);
    }

    /** 绑定主库存补偿队列。 */
    @Bean
    public Binding compensateBinding() {
        return BindingBuilder.bind(compensateQueue()).to(compensateExchange()).with(COMPENSATE_ROUTING_KEY);
    }
}
