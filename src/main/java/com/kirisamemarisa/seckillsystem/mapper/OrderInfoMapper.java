package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    @Select("SELECT * FROM t_order WHERE id = #{orderId} FOR UPDATE")
    OrderInfo selectByIdForUpdate(@Param("orderId") Long orderId);
}