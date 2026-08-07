package com.kirisamemarisa.seckillsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AddGoodsVo {
    @NotBlank(message = "商品名称不能为空且不能全为空格")
    private String goodsName;
    private String goodsTitle;
    private String goodsImg;
    private String goodsDetail;

    @NotNull(message = "商品原价不能为空")
    @DecimalMin(value = "0.0", message = "价格不能为负数")
    private BigDecimal goodsPrice;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 1, message = "普通库存至少为1")
    private Integer goodsStock;

    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.0", message = "秒杀价不能为负")
    private BigDecimal seckillPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "秒杀库存至少需要分配1件")
    private Integer seckillStock;

    @NotNull(message = "秒杀开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startDate;

    @NotNull(message = "秒杀结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endDate;

    @AssertTrue(message = "无效的活动时间：秒杀结束时间必须晚于开始时间")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }
}