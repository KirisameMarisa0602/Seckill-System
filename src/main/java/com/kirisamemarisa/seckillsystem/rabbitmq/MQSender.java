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

@Service
@Slf4j
public class MQSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;
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
    public void sendSeckillMessageReliable(SeckillMessage message) throws Exception {
        sendSeckillMessageReliable(message, 0);
    }
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