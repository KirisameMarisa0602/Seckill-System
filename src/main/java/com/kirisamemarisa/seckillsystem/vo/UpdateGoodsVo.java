package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class UpdateGoodsVo {
    @NotNull(message = "商品ID不能为空")
    private Long id;

    private String goodsName;
    private String goodsTitle;
    private String goodsImg;
    private String goodsDetail;

    @DecimalMin(value = "0.0", message = "价格不能为负数")
    private BigDecimal goodsPrice;

    @Min(value = 1, message = "普通库存至少为1")
    private Integer goodsStock;

    @DecimalMin(value = "0.0", message = "秒杀价格不能为负")
    private BigDecimal seckillPrice;

    @Min(value = 0, message = "秒杀库存不能为负")
    private Integer seckillStock;

    private Date startDate;
    private Date endDate;
}