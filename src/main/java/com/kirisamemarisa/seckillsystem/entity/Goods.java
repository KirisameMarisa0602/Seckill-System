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

/**
 * 普通商品，对应表 {@code t_goods}。
 *
 * <p>秒杀价、秒杀库存和时间窗口在 {@link SeckillGoods}，不在本表。
 * {@link com.kirisamemarisa.seckillsystem.vo.GoodsVo} 把两张表拼成前端商品卡片。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_goods")
public class Goods implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键。秒杀、订单、缓存都用这个 ID 关联。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商品名称，列表和订单上都展示它。 */
    private String goodsName;
    /** 副标题，可空。 */
    private String goodsTitle;
    /** 封面图 URL，可空；前端无图时显示占位图标。 */
    private String goodsImg;
    /** 详情文案，可空。 */
    private String goodsDetail;
    /** 日常原价。秒杀价必须低于或等于它。 */
    private BigDecimal goodsPrice;
    /**
     * 主库存。秒杀预扣的是 {@code t_seckill_goods.stock_count}，本字段在支付成功时才减 1。
     */
    private Integer goodsStock;
}
