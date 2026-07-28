package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

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
    public void sendSeckillMessage(SeckillMessage message) {
        log.info("【收银员操作】接收到秒杀请求，正在投递消息至 MQ: {}", message);
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE, RabbitMQConfig.ROUTING_KEY, message);
    }
}