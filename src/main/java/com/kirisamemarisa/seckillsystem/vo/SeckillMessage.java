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
    /** 下单用户 ID。 */
    private Long userId;
    /** 秒杀商品 ID。 */
    private Long goodsId;
    /** 下单时快照的商品名，避免消费者再查详情。 */
    private String goodsName;
    /** 下单时快照的秒杀价。 */
    private BigDecimal seckillPrice;
}
