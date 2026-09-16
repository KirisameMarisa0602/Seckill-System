package com.kirisamemarisa.seckillsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水，对应表 {@code t_payment_record}。{@code trade_no} 唯一，支付宝异步通知靠它做幂等。
 */
@Data
@TableName("t_payment_record")
public class PaymentRecord {
    /** 雪花 ID，JSON 输出为字符串。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    /** 关联 {@code t_order.id}，同样字符串化以免前端精度丢失。 */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;
    /** 支付宝交易号，表上唯一，异步通知靠它做幂等。 */
    private String tradeNo;
    /** 实付金额，须与订单秒杀价一致。 */
    private BigDecimal amount;
    /** 收款应用 APPID。 */
    private String appId;
    /** 卖家支付宝账号（PID），可空。 */
    private String sellerId;
    /** {@code PAID} 已入账；{@code REFUND_PENDING} 关单后到账或主库存不足，待人工退款。 */
    private String status;

    /** 流水创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createDate;

    /** 最近更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateDate;
}
