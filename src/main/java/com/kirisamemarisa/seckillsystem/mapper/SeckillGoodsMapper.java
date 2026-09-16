package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import org.apache.ibatis.annotations.Update;

/**
 * {@code t_seckill_goods} Mapper。增减秒杀预扣库存用注解 SQL，不走 XML。
 */
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {
    /**
     * 下单预扣秒杀库存。基于数据库的乐观锁：{@code stock_count > 0} 条件更新，
     * 影响行数为 0 表示已被抢光，调用方应回滚事务。
     */
    @Update("UPDATE t_seckill_goods SET stock_count = stock_count - 1 WHERE goods_id = #{goodsId} AND stock_count > 0")
    int decrementStock(Long goodsId);

    /** 超时关单回补秒杀库存，无上限条件（与下单时的 -1 对冲）。 */
    @Update("UPDATE t_seckill_goods SET stock_count = stock_count + 1 WHERE goods_id = #{goodsId}")
    int incrementStock(Long goodsId);
}
