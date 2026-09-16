package com.kirisamemarisa.seckillsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀下单 MQ 消息体。热路径 Redis 预扣成功后写入 Outbox，再投递到 RabbitMQ。
 * 消费者用 {@code userId + goodsId} 落 {@code t_order}/{@code t_seckill_order}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {
    private Long userId;
    private Long goodsId;
    private String goodsName;
    private BigDecimal seckillPrice;
}
