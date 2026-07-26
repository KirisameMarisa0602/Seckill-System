package com.kirisamemarisa.seckillsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goods implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String goodsName;
    private String goodsTitle;
    private String goodsImg;
    private String goodsDetail;
    private BigDecimal goodsPrice; // 注意精度丢失问题，必须用BigDecimal
    private Integer goodsStock;
}