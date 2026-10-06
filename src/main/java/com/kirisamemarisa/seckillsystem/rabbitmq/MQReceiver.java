package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.exception.InsufficientStockException;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MQReceiver {
    @Autowired private IOrderService orderService;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private MQSender mqSender;

    @Autowired private SeckillOrderMapper seckillOrderMapper;

    @Autowired private DefaultRedisScript<Long> rollbackSeckillScript;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void receive(SeckillMessage seckillMessage, Channel channel, Message message) throws IOException {
        Long userId = seckillMessage.getUserId();
        Long goodsId = seckillMessage.getGoodsId();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            SeckillOrder existingOrder = findExistingOrder(userId, goodsId);
            if (existingOrder != null) {
                acknowledgeExisting(existingOrder, seckillMessage);
                channel.basicAck(deliveryTag, false);
                return;
            }
            GoodsVo mockGoodsVo = new GoodsVo();
            mockGoodsVo.setId(goodsId);
            mockGoodsVo.setGoodsName(seckillMessage.getGoodsName());
            mockGoodsVo.setSeckillPrice(seckillMessage.getSeckillPrice());
            OrderInfo orderInfo = orderService.createSeckillOrder(userId, mockGoodsVo, seckillMessage.getEventId());
            try {
                mqSender.sendDelayOrderMessageReliable(orderInfo.getId());
            } catch (Exception e) {
                log.error("延迟关单消息投递失败，订单 {} 将由数据库扫描任务兜底", orderInfo.getId(), e);
            }
            log.info("【MQReceiver】订单真实落库成功：用户{}，商品{}", userId, goodsId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            if (findCause(e, InsufficientStockException.class) != null) {
                log.warn("DB 落库确认无库存，将 Redis 可售库存对齐为 0：商品{}", goodsId);
                stringRedisTemplate.opsForValue().set(
                        GoodsKey.getSeckillGoodsStock.getPrefix() + goodsId, "0");
                stringRedisTemplate.opsForValue().set(GoodsKey.isStockEmpty.getPrefix() + goodsId, "1");
                stringRedisTemplate.delete(OrderKey.seckillUserOrder.getPrefix() + userId + ":" + goodsId);
                channel.basicAck(deliveryTag, false);
            } else if (findCause(e, DuplicateKeyException.class) != null) {
                SeckillOrder existingOrder = findExistingOrder(userId, goodsId);
                if (existingOrder != null) {
                    acknowledgeExisting(existingOrder, seckillMessage);
                    channel.basicAck(deliveryTag, false);
                } else {
                    channel.basicNack(deliveryTag, false, false);
                }
            } else {
                log.error("订单消费发生未知异常，投入错误死信队列", e);
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ERROR_DEAD_LETTER_QUEUE)
    public void receiveErrorDeadLetter(SeckillMessage seckillMessage, Channel channel,
                                       Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Long userId = seckillMessage.getUserId();
        Long goodsId = seckillMessage.getGoodsId();
        try {
            SeckillOrder existingOrder = findExistingOrder(userId, goodsId);
            if (existingOrder != null) {
                acknowledgeExisting(existingOrder, seckillMessage);
                channel.basicAck(deliveryTag, false);
                return;
            }

            int retryCount = getRetryCount(message);
            if (retryCount < 3) {
                mqSender.sendSeckillMessageReliable(seckillMessage, retryCount + 1);
                channel.basicAck(deliveryTag, false);
                return;
            }

            rollbackReservation(userId, goodsId);
            log.error("秒杀消息达到最大重试次数，已回补 Redis 预留库存。用户: {}, 商品: {}",
                    userId, goodsId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("错误死信处理失败，消息保留等待稍后处理", e);
            channel.basicNack(deliveryTag, false, true);
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

    private void acknowledgeExisting(SeckillOrder existing, SeckillMessage message) {
        cacheOrder(existing);
        String incomingEventId = message.getEventId();
        if (incomingEventId == null || incomingEventId.equals(existing.getEventId())) {
            return;
        }
        rollbackReservation(message.getUserId(), message.getGoodsId());
        stringRedisTemplate.opsForValue().set(
                OrderKey.seckillUserOrder.getPrefix() + message.getUserId() + ":" + message.getGoodsId(),
                "1");
        log.warn("用户 {} 商品 {} 已有订单，已回补本次多扣的 Redis 库存", message.getUserId(), message.getGoodsId());
    }

    private SeckillOrder findExistingOrder(Long userId, Long goodsId) {
        return seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>()
                .eq("user_id", userId)
                .eq("goods_id", goodsId)
                .last("LIMIT 1"));
    }

    private void cacheOrder(SeckillOrder order) {
        redisTemplate.opsForValue().set(
                OrderKey.seckillOrderCache.getPrefix() + order.getUserId() + ":" + order.getGoodsId(),
                order.getOrderId(),
                OrderKey.seckillOrderCache.expireSeconds(),
                TimeUnit.SECONDS
        );
    }

    private int getRetryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get("x-app-retry-count");
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private void rollbackReservation(Long userId, Long goodsId) {
        Long restored = stringRedisTemplate.execute(
                rollbackSeckillScript,
                Arrays.asList(
                        GoodsKey.getSeckillGoodsStock.getPrefix() + goodsId,
                        OrderKey.seckillUserOrder.getPrefix() + userId + ":" + goodsId,
                        GoodsKey.isStockEmpty.getPrefix() + goodsId
                )
        );
        if (restored != null && restored > 0) {
            stringRedisTemplate.convertAndSend("stock_replenish_channel", goodsId.toString());
        }
    }

    private static <T extends Throwable> T findCause(Throwable error, Class<T> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
