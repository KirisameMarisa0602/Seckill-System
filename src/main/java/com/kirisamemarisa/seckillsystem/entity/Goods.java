package com.kirisamemarisa.seckillsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

//自动生成所有属性的 get/set 方法，以及 toString()、equals()、hashCode()
@Data
//无参构造函数
@NoArgsConstructor
//全参构造函数
@AllArgsConstructor
//建造者模式，针对无参和全参之间一个或多个参数的构造函数
//eg：Order order = Order.builder().userId(1L).goodsId(2L).build();
@Builder

//解决实体类名和数据库表名不一致的问题
@TableName("t_goods")

//实现序列化标签接口，为后续放入redis铺垫
public class Goods implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)

    private Long id;
    private String goodsName;
    private String goodsTitle;
    private String goodsImg;
    private String goodsDetail;
    private BigDecimal goodsPrice;
    private Integer goodsStock;
}