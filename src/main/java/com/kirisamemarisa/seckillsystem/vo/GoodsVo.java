package com.kirisamemarisa.seckillsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品视图：继承 {@link Goods} 后再拼秒杀价、可售库存和时间窗口。
 * 对应 {@code t_goods} JOIN {@code t_seckill_goods}；缓存穿透占位会把 {@code id} 写成 {@code -1}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GoodsVo extends Goods {
    /** 秒杀价，来自 {@code t_seckill_goods.seckill_price}。 */
    private BigDecimal seckillPrice;
    /** 秒杀可售库存，来自 {@code t_seckill_goods.stock_count}，不是主库存。 */
    private Integer stockCount;

    /** 开抢时间，JSON 固定 {@code yyyy-MM-dd HH:mm:ss}（东八区）。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startDate;

    /** 结束时间，同样格式。前端用它和库存判断即将开始 / 抢购中 / 已结束。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endDate;
}
