package com.kirisamemarisa.seckillsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员热更新商品入参。除 {@code id} 外均为可选，{@code null} 表示该字段不改。
 * 库存能否改还要看活动是否进行中、有无待支付单，那是 Service 层的契约。
 */
@Data
public class UpdateGoodsVo {
    @NotNull(message = "商品ID不能为空")
    private Long id;

    /** 以下字段为 {@code null} 表示不改。 */
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endDate;
}
