package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.mapper.GoodsMapper;
import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
import com.kirisamemarisa.seckillsystem.redis.OrderKey;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MQReceiver {
    @Autowired private IOrderService orderService;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private MQSender mqSender;

    @Autowired private GoodsMapper goodsMapper;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void receive(SeckillMessage seckillMessage, Channel channel, Message message) throws IOException {
        Long userId = seckillMessage.getUserId();
        Long goodsId = seckillMessage.getGoodsId();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String mqIdempotentKey = "mq:consume:lock:" + userId + ":" + goodsId;
        try {
            Boolean isFirstConsume = stringRedisTemplate.opsForValue().setIfAbsent(mqIdempotentKey, "1", 5, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(isFirstConsume)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            Boolean hasOrder = redisTemplate.hasKey(OrderKey.seckillOrderCache.getPrefix() + userId + ":" + goodsId);
            if (Boolean.TRUE.equals(hasOrder)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            GoodsVo mockGoodsVo = new GoodsVo();
            mockGoodsVo.setId(goodsId);
            mockGoodsVo.setGoodsName(seckillMessage.getGoodsName());
            mockGoodsVo.setSeckillPrice(seckillMessage.getSeckillPrice());
            OrderInfo orderInfo = orderService.createSeckillOrder(userId, mockGoodsVo);
            mqSender.sendDelayOrderMessage(orderInfo.getId());
            log.info("【MQReceiver】订单真实落库成功：用户{}，商品{}", userId, goodsId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("库存不足")) {
                log.warn("【MQReceiver】DB 落库确认无库存，触发熔断，停止后续请求：商品{}", goodsId);
                stringRedisTemplate.opsForValue().set(GoodsKey.isStockEmpty.getPrefix() + goodsId, "1");
            } else if (e instanceof DuplicateKeyException || (e.getCause() != null && e.getCause() instanceof DuplicateKeyException)) {
                log.warn("【幂等防漏触发】捕捉到数据重复插入冲突，用户:{}, 商品:{}", userId, goodsId);
            } else {
                log.error("【MQReceiver】订单消费发生未知异常，已终止该请求：{}", e.getMessage());
            }
            stringRedisTemplate.delete(OrderKey.seckillUserOrder.getPrefix() + userId + ":" + goodsId);
            channel.basicAck(deliveryTag, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void receiveDeadLetter(Long orderId, Channel channel, Message message) throws IOException {
        log.warn("【MQReceiver: 触发死信关单】收到超时未支付倒计时结束的订单ID：{}", orderId);
        try {
            orderService.cancelTimeoutOrder(orderId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("【超时关单异常】需人工介入手动回滚！订单ID: {}", orderId, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.COMPENSATE_QUEUE)
    public void receiveCompensate(Long orderId, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo == null) {
                channel.basicAck(deliveryTag, false); return;
            }
            if (goodsMapper.decrementGoodsStock(orderInfo.getGoodsId()) > 0) {
                log.info("【容错补偿节点】订单 {} 主库存补偿成功！", orderId);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("【致命错误】补偿重试节点发生异常，拒绝 ACK 回到队列盲目重试！", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}