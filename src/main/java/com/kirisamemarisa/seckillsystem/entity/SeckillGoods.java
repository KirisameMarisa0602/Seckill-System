package com.kirisamemarisa.seckillsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀场次，对应表 {@code t_seckill_goods}。与 {@code t_goods} 一对一（{@code goods_id} 唯一）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_seckill_goods")
public class SeckillGoods implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long goodsId;
    private BigDecimal seckillPrice;
    /** 秒杀可售库存（预扣）。下单减、超时关单加；与 Redis 可售库存应对齐。 */
    private Integer stockCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
