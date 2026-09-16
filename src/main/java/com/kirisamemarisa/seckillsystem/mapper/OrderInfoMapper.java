package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code t_order} Mapper。关单/支付入账需要行锁时走 {@link #selectByIdForUpdate}。
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    /**
     * {@code SELECT ... FOR UPDATE} 锁住该订单行，直到当前事务结束。
     * 必须在已有事务里调用，否则锁立刻释放，关单与支付回调会竞态。
     */
    @Select("SELECT * FROM t_order WHERE id = #{orderId} FOR UPDATE")
    OrderInfo selectByIdForUpdate(@Param("orderId") Long orderId);
}
