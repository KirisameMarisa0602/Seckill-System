package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MQSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendSeckillMessage(SeckillMessage message) {
        log.info("【收银员操作】接收到秒杀请求，正在打印小票（发送消息）: {}", message);
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE, "seckill.message", message);
    }
}