package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.User;
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
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DuplicateKeyException;

@Service
@Slf4j
public class MQReceiver {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void receive(SeckillMessage seckillMessage, Channel channel, Message message) throws IOException {
        log.info("【MQReceiver】从队列中拿到了一张订单，准备落库：{}", seckillMessage);

        Long userId = seckillMessage.getUserId();
        Long goodsId = seckillMessage.getGoodsId();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // ==========================================
            // 【核心修复】：使用 SETNX 替代原来的 hasKey 检查
            // 如果并发来两条消息，只有第一条能成功写入并返回 true。这把锁只要10秒，足够挡住瞬时的重复投递
            // ==========================================
            String mqIdempotentKey = "mq:consume:lock:" + userId + ":" + goodsId;
            Boolean isFirstConsume = redisTemplate.opsForValue().setIfAbsent(mqIdempotentKey, "1", 10, TimeUnit.SECONDS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("【MQ 消费幂等拦截】该订单正在处理中或已处理，直接 ACK 丢弃。用户ID:{}, 商品ID:{}", userId, goodsId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 二次校验，防止重复购买（依然保留，作为兜底）
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

            // 1. 创建真实秒杀订单
            OrderInfo orderInfo = orderService.createSeckillOrder(userId, goodsVo);

            // 2. 【核心新增】如果下单成功，立即丢进延迟队列开启 1 分钟倒计时！
            if (orderInfo != null) {
                mqSender.sendDelayOrderMessage(orderInfo.getId());
            }

            log.info("【MQReceiver】订单真实落库成功：用户{}，商品{}", userId, goodsId);
            channel.basicAck(deliveryTag, false);
        } catch (DuplicateKeyException e) {
            log.warn("【幂等拦截机制触发】数据库兜底拦截到恶意重投/重复消费，完美化解！用户:{}, 商品:{}", userId, goodsId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("【MQReceiver】订单消费发生未知异常，抛弃或转入死信队列：{}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 【核心新增】：监听死信队列，收到到达 TTL 时限的死亡消息，调用超时关单
     */
    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void receiveDeadLetter(Long orderId, Channel channel, Message message) throws IOException {
        log.warn("【MQReceiver: 触发死信关单】收到超时未支付倒计时结束的订单ID：{}", orderId);
        try {
            orderService.cancelTimeoutOrder(orderId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("【超时关单异常】，打回死信队列重试", e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}