package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;

/**
 * {@code t_seckill_order} Mapper。一人一单靠表上 {@code uk_seckill_order_user_goods}，无自定义 SQL。
 */
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
}
