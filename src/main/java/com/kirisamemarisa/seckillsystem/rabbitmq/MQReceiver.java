package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.dao.DuplicateKeyException;

@Service
@Slf4j
public class MQReceiver {
    @Autowired private IGoodsService goodsService;

    @Autowired private IOrderService orderService;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private MQSender mqSender;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void receive(SeckillMessage seckillMessage, Channel channel, Message message) throws IOException {
        log.info("【MQReceiver】从队列中拿到了一张订单，准备落库：{}", seckillMessage);
        Long userId = seckillMessage.getUserId();
        Long goodsId = seckillMessage.getGoodsId();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String mqIdempotentKey = "mq:consume:lock:" + userId + ":" + goodsId;
            Boolean isFirstConsume = stringRedisTemplate.opsForValue().setIfAbsent(mqIdempotentKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("【MQ 消费幂等拦截】该订单正在处理中或已处理，直接 ACK 丢弃。用户ID:{}, 商品ID:{}", userId, goodsId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            Boolean hasOrder = redisTemplate.hasKey("seckillOrderCache:" + userId + ":" + goodsId);
            if (Boolean.TRUE.equals(hasOrder)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
            if (goodsVo.getStockCount() < 1) {
                log.warn("【MQReceiver】真实库存已售罄！商品ID：{}", goodsId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            OrderInfo orderInfo = orderService.createSeckillOrder(userId, goodsVo);
            if (orderInfo != null) {
                mqSender.sendDelayOrderMessage(orderInfo.getId());
                log.info("【MQReceiver】订单真实落库成功：用户{}，商品{}", userId, goodsId);
            } else {
                log.warn("【MQReceiver】DB 落库失败(无库存)，触发兜底状态清理！释放用户{}在商品{}的限购锁", userId, goodsId);
                stringRedisTemplate.delete("seckillUserOrder:" + userId + ":" + goodsId);
                stringRedisTemplate.opsForValue().set("isStockEmpty:" + goodsId, "1");
            }
            channel.basicAck(deliveryTag, false);
        } catch (DuplicateKeyException e) {
            log.warn("【幂等拦截机制触发】数据库兜底拦截到恶意重投/重复消费，完美化解！用户:{}, 商品:{}", userId, goodsId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("【MQReceiver】订单消费发生未知异常，抛弃或转入死信队列：{}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void receiveDeadLetter(Long orderId, Channel channel, Message message) throws IOException {
        log.warn("【MQReceiver: 触发死信关单】收到超时未支付倒计时结束的订单ID：{}", orderId);
        try {
            orderService.cancelTimeoutOrder(orderId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("【超时关单异常】，死信丢弃，需人工介入补偿！订单ID: {}", orderId, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}