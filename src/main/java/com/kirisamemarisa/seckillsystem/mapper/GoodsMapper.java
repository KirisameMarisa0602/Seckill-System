package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 查询带有秒杀信息的商品列表
     */
    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_date, sg.end_date " +
            "FROM t_goods g LEFT JOIN t_seckill_goods sg ON g.id = sg.goods_id")
    List<GoodsVo> findGoodsVo();

    /**
     * 根据商品ID查询详细的秒杀商品信息
     */
    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_date, sg.end_date " +
            "FROM t_goods g LEFT JOIN t_seckill_goods sg ON g.id = sg.goods_id " +
            "WHERE g.id = #{goodsId}")
    GoodsVo findGoodsVoByGoodsId(Long goodsId);
}