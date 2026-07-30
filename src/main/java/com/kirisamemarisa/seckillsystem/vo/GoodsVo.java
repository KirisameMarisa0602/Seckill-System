package com.kirisamemarisa.seckillsystem.vo;

import com.kirisamemarisa.seckillsystem.entity.Goods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor

//比对两个 GoodsVo 对象是否相等时，不仅要比对它自己的“秒杀属性”，还必须去比对它从父类继承来的“基础商品属性”
@EqualsAndHashCode(callSuper = true)

//继承父类Goods
public class GoodsVo extends Goods {
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private Date startDate;
    private Date endDate;
}