package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.redis.SeckillKey;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀 Outbox 补偿发布器。
 *
 * <p>热路径 Lua 预扣库存时写入 Redis Outbox（ZSET {@link SeckillKey#outboxPending}
 * + HASH {@link SeckillKey#outboxEvent}），本组件定时扫描到期事件，
 * 经 {@link MQSender} 投递到 {@code seckillExchange}，成功后再删除 Outbox 记录。
 *
 * <p>投递失败按重试次数指数退避，重新写入 ZSET score；{@link SeckillKey#outboxLock}
 * 保证多实例不会并发发布同一 eventId。
 */
@Slf4j
@Component
public class SeckillOutboxPublisher {
    private static final int BATCH_SIZE = 100;

    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private MQSender mqSender;

    /**
     * 扫描 score ≤ 当前时间的待投递事件并逐条发布。
     */
    @Scheduled(fixedDelayString = "${seckill.outbox.poll-interval-ms:200}")
    public void publishPendingEvents() {
        long now = System.currentTimeMillis();
        Set<String> eventIds = stringRedisTemplate.opsForZSet().rangeByScore(
                SeckillKey.outboxPending.getPrefix(), 0, now, 0, BATCH_SIZE);
        if (eventIds == null || eventIds.isEmpty()) {
            return;
        }
        for (String eventId : eventIds) {
            publishOne(eventId);
        }
    }

    /**
     * 抢锁后读取事件 HASH，可靠投递 MQ；成功删除 Outbox，失败则推迟 score 后重试。
     */
    private void publishOne(String eventId) {
        String lockKey = SeckillKey.outboxLock.getPrefix() + eventId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey, "1", SeckillKey.outboxLock.expireSeconds(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        String eventKey = SeckillKey.outboxEvent.getPrefix() + eventId;
        try {
            Map<Object, Object> values = stringRedisTemplate.opsForHash().entries(eventKey);
            if (values.isEmpty()) {
                stringRedisTemplate.opsForZSet().remove(SeckillKey.outboxPending.getPrefix(), eventId);
                return;
            }
            SeckillMessage message = new SeckillMessage(
                    Long.valueOf(required(values, "userId")),
                    Long.valueOf(required(values, "goodsId")),
                    required(values, "goodsName"),
                    new BigDecimal(required(values, "seckillPrice"))
            );
            mqSender.sendSeckillMessageReliable(message);
            stringRedisTemplate.opsForZSet().remove(SeckillKey.outboxPending.getPrefix(), eventId);
            stringRedisTemplate.delete(eventKey);
        } catch (Exception e) {
            int retryCount = incrementRetryCount(eventKey);
            long backoffMillis = Math.min(60_000L, 1_000L << Math.min(retryCount, 6));
            stringRedisTemplate.opsForZSet().add(
                    SeckillKey.outboxPending.getPrefix(),
                    eventId,
                    System.currentTimeMillis() + backoffMillis
            );
            log.error("秒杀 Outbox 事件 {} 第 {} 次投递失败，将延迟重试", eventId, retryCount, e);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /** 累加事件 HASH 中的 {@code retryCount}，供退避计算。 */
    private int incrementRetryCount(String eventKey) {
        Long count = stringRedisTemplate.opsForHash().increment(eventKey, "retryCount", 1);
        return count == null ? 1 : count.intValue();
    }

    /** 读取 Outbox 必填字段，缺失则视为损坏事件。 */
    private String required(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            throw new IllegalStateException("Outbox 事件缺少字段: " + key);
        }
        return value.toString();
    }
}
