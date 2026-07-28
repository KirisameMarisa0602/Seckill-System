package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import org.apache.ibatis.annotations.Update;

public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {
    @Update("UPDATE t_seckill_goods SET stock_count = stock_count - 1 WHERE goods_id = #{goodsId} AND stock_count > 0")
    int decrementStock(Long goodsId);
}