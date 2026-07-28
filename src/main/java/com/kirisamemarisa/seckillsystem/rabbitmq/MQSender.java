package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息发送者 (前台收银员)
 */
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀信息进入队列
     */
    public void sendSeckillMessage(SeckillMessage message) {
        log.info("【收银员操作】接收到秒杀请求，正在打印小票（发送消息）: {}", message);

        // 参数1: 交换机名称 (前台领班)
        // 参数2: RoutingKey (小票上的标签，需匹配主题交换机的拦截规则 'seckill.#')
        // 参数3: 具体的载体数据 (即我们刚定义的包含 User 和 GoodsId 的实体)
        // 底层会自动调用我们配置的 Jackson 序列化器，将其转为漂亮的 JSON
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE, "seckill.message", message);
    }
}