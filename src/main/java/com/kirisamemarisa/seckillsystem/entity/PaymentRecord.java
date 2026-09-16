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
    private String tradeNo;
    private BigDecimal amount;
    private String appId;
    private String sellerId;
    /** {@code PAID} 已入账；{@code REFUND_PENDING} 关单后到账或主库存不足，待人工退款。 */
    private String status;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
}
