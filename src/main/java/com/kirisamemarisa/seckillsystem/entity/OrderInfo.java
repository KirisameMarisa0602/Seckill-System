package com.kirisamemarisa.seckillsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单，对应表 {@code t_order}。秒杀一单一件；主键雪花 ID。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_order")
public class OrderInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 雪花 ID。{@code STRING} 避免前端 JS Number 丢精度。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /** 下单用户，即 {@code t_user.id}（手机号）。 */
    private Long userId;
    /** 秒杀商品 ID。 */
    private Long goodsId;
    /** 收货地址占位，当前业务未接地址模块，固定写 0。 */
    private Long deliveryAddrId;
    /** 下单时快照的商品名。 */
    private String goodsName;
    /** 购买件数。秒杀路径固定 1。 */
    private Integer goodsCount;
    /** 成交单价，取秒杀价。 */
    private BigDecimal goodsPrice;
    /** 下单渠道。秒杀路径写 1。 */
    private Integer orderChannel;
    /**
     * 订单状态：{@code 0} 待支付，{@code 1} 已支付，{@code -1} 超时取消，{@code -2} 待退款。
     */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime payDate;
}
