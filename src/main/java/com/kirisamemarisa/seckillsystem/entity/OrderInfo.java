package com.kirisamemarisa.seckillsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Long goodsId;
    private Long deliveryAddrId;
    private String goodsName;
    private Integer goodsCount;
    private BigDecimal goodsPrice;
    private Integer orderChannel;
    private Integer status; // 0未支付，1已支付，2已发货，3已收货，4已退款，5已完成
    private Date createDate;
    private Date payDate;
}