package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * {@code t_goods} Mapper。联表查出秒杀视图 {@link GoodsVo}；主库存扣减用条件更新防超卖。
 */
public interface GoodsMapper extends BaseMapper<Goods> {
    /** {@code t_goods} INNER JOIN {@code t_seckill_goods}，只返回已配置秒杀场次的商品。 */
    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_date, sg.end_date " +
            "FROM t_goods g INNER JOIN t_seckill_goods sg ON g.id = sg.goods_id")
    List<GoodsVo> findGoodsVo();

    /** 按商品主键联表查一条秒杀视图。 */
    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_date, sg.end_date " +
            "FROM t_goods g INNER JOIN t_seckill_goods sg ON g.id = sg.goods_id " +
            "WHERE g.id = #{goodsId}")
    GoodsVo findGoodsVoByGoodsId(Long goodsId);

    /**
     * 支付成功时扣主库存。{@code goods_stock > 0} 保证并发下不会减成负数；返回值是影响行数。
     */
    @Update("UPDATE t_goods SET goods_stock = goods_stock - 1 WHERE id = #{goodsId} AND goods_stock > 0")
    int decrementGoodsStock(Long goodsId);

    /** 已配置秒杀场次的商品总数。 */
    @Select("SELECT count(1) FROM t_goods g INNER JOIN t_seckill_goods sg ON g.id = sg.goods_id")
    long countSeckillGoods();

    /** 同上联表，附加 {@code LIMIT offset, size} 给后台分页。 */
    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_date, sg.end_date " +
            "FROM t_goods g INNER JOIN t_seckill_goods sg ON g.id = sg.goods_id " +
            "LIMIT #{offset}, #{size}")
    List<GoodsVo> findGoodsVoByLimit(@org.apache.ibatis.annotations.Param("offset") int offset,
                                     @org.apache.ibatis.annotations.Param("size") int size);
}
