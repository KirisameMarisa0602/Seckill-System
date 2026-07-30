package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class AddGoodsVo {
    @NotNull(message = "商品名称不能为空")
    private String goodsName;
    private String goodsTitle;
    private String goodsImg;
    private String goodsDetail;

    @NotNull(message = "商品原价不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal goodsPrice;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 1, message = "普通库存至少为1")
    private Integer goodsStock;

    @NotNull(message = "秒杀价格不能为空")
    private BigDecimal seckillPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "秒杀库存至少需要分配1件")
    private Integer seckillStock;

    @NotNull(message = "秒杀开始时间不能为空")
    private Date startDate;

    @NotNull(message = "秒杀结束时间不能为空")
    private Date endDate;
}