package com.kirisamemarisa.seckillsystem.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.PaymentRecord;
import com.kirisamemarisa.seckillsystem.mapper.PaymentRecordMapper;
import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
import com.kirisamemarisa.seckillsystem.redis.SeckillKey;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务 Gauge：Outbox 积压、待退款笔数、Redis/MySQL 秒杀库存不一致数。
 *
 * <p>供 Prometheus {@code /actuator/prometheus} 采集，不参与交易路径。
 * 默认 30s 刷新；库存不一致且 Outbox 为空时打 warn，提示可能需要人工对账。
 * 依赖中间件：Redis、MySQL、Micrometer。无 {@code @Order}，{@code @PostConstruct} 注册表盘。
 */
@Slf4j
@Component
public class BusinessMetrics {
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private PaymentRecordMapper paymentRecordMapper;
    @Autowired private IGoodsService goodsService;

    private final AtomicLong outboxPending = new AtomicLong();
    private final AtomicLong refundPending = new AtomicLong();
    private final AtomicLong inventoryMismatch = new AtomicLong();

    /**
     * 把三个 AtomicLong 注册为 Gauge，采集端读到的是刷新后的快照值。
     */
    @PostConstruct
    public void register() {
        meterRegistry.gauge("seckill.outbox.pending", outboxPending);
        meterRegistry.gauge("seckill.payment.refund.pending", refundPending);
        meterRegistry.gauge("seckill.inventory.mismatch", inventoryMismatch);
    }

    /**
     * 定时从 Redis ZSET、支付表、商品列表重算 Gauge。
     */
    @Scheduled(fixedDelayString = "${seckill.metrics.refresh-interval-ms:30000}")
    public void refresh() {
        Long outboxSize = stringRedisTemplate.opsForZSet().zCard(SeckillKey.outboxPending.getPrefix());
        outboxPending.set(outboxSize == null ? 0 : outboxSize);
        refundPending.set(paymentRecordMapper.selectCount(
                new QueryWrapper<PaymentRecord>().eq("status", "REFUND_PENDING")));

        long mismatchCount = 0;
        for (GoodsVo goods : goodsService.findGoodsVo()) {
            String redisStock = stringRedisTemplate.opsForValue().get(
                    GoodsKey.getSeckillGoodsStock.getPrefix() + goods.getId());
            if (redisStock == null || goods.getStockCount() == null) {
                mismatchCount++;
                continue;
            }
            try {
                if (Long.parseLong(redisStock) != goods.getStockCount().longValue()) {
                    mismatchCount++;
                }
            } catch (NumberFormatException e) {
                mismatchCount++;
            }
        }
        inventoryMismatch.set(mismatchCount);
        if (mismatchCount > 0 && outboxPending.get() == 0) {
            log.warn("检测到 {} 个商品的 Redis 与 MySQL 秒杀库存不一致，请检查 MQ 在途消息后执行对账",
                    mismatchCount);
        }
    }
}
