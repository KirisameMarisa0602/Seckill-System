package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀 MQ 可靠发送器。
 *
 * <p>投递目标：
 * <ul>
 *   <li>秒杀下单：交换机 {@code seckillExchange}，路由键 {@code seckill.message}，队列 {@code seckillQueue}</li>
 *   <li>延迟关单：交换机 {@code seckill.delay.exchange} → TTL 队列 {@code seckill.delay.queue}（15 分钟）
 *       → 死信交换机 {@code seckill.dlx.exchange} → {@code seckill.dlq.queue}</li>
 *   <li>主库存补偿：交换机 {@code seckill.compensate.exchange} → {@code seckill.compensate.queue}</li>
 * </ul>
 * 发送后等待 Publisher Confirm，未 ACK 或被 Return 则抛错，由 Outbox 发布器退避重试。
 */
@Service
@Slf4j
public class MQSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 注册 Confirm / Return 回调，记录到达交换机失败或无法路由到队列的情况。
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("【致命错误】可靠性投递失败！消息未到达 Exchange，原因: {}", cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.error("【路由异常】消息已到达交换机，但找不到对应队列。消息内容: {}, 交换机: {}, 路由键: {}",
                    new String(returnedMessage.getMessage().getBody()),
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey());
        });
    }

    /**
     * 将秒杀 Outbox 事件投递到 {@code seckillExchange}（首次，重试计数为 0）。
     */
    public void sendSeckillMessageReliable(SeckillMessage message) throws Exception {
        sendSeckillMessageReliable(message, 0);
    }

    /**
     * 可靠投递秒杀下单消息。
     *
     * @param retryCount 写入消息头 {@code x-app-retry-count}，错误死信消费时用于控制重投次数
     */
    public void sendSeckillMessageReliable(SeckillMessage message, int retryCount) throws Exception {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SEND_ROUTING_KEY,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setHeader("x-app-retry-count", retryCount);
                    return amqpMessage;
                },
                correlationData
        );
        CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck() || correlationData.getReturned() != null) {
            throw new IllegalStateException("秒杀消息未可靠路由: " + confirm.getReason());
        }
    }

    /**
     * 投递延迟关单消息：进入 TTL 队列，到期后转入死信队列由 {@link MQReceiver#receiveDeadLetter} 关单。
     */
    public void sendDelayOrderMessageReliable(Long orderId) throws Exception {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.DELAY_ROUTING_KEY,
                orderId,
                correlationData
        );
        CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck() || correlationData.getReturned() != null) {
            throw new IllegalStateException("延迟关单消息未可靠路由: " + confirm.getReason());
        }
    }

    /**
     * 投递支付后主库存补偿消息到 {@code seckill.compensate.exchange}。
     *
     * @param retryCount 写入消息头，补偿消费失败时控制重投次数
     */
    public void sendCompensateMessageReliable(Long orderId, int retryCount) throws Exception {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.COMPENSATE_EXCHANGE,
                RabbitMQConfig.COMPENSATE_ROUTING_KEY,
                orderId,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setHeader("x-app-retry-count", retryCount);
                    return amqpMessage;
                },
                correlationData
        );
        CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck() || correlationData.getReturned() != null) {
            throw new IllegalStateException("补偿消息未可靠路由: " + confirm.getReason());
        }
    }
}
