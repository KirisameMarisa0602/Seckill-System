package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import org.apache.ibatis.annotations.Select;
import java.util.List;

//继承BaseMapper<泛型>，Mybatis-Plus扫描泛型获取对应的实体类，反射获取MySQL数据表名、表字段，完成持久化
public interface GoodsMapper extends BaseMapper<Goods> {
    //扩展的SQL语句：业务层要看抢购商品信息的同时一般要和原价商品比对着看
    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_date, sg.end_date " +
            "FROM t_goods g LEFT JOIN t_seckill_goods sg ON g.id = sg.goods_id")
    //结果返回给GoodsVo（视图对象）而不是Goods，防止污染Goods实体类/t_goods数据库表，同时不用把所有字段都返回，返回一些前端需要的字段给VO对象就行
    List<GoodsVo> findGoodsVo();

    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_date, sg.end_date " +
            "FROM t_goods g LEFT JOIN t_seckill_goods sg ON g.id = sg.goods_id " +
            "WHERE g.id = #{goodsId}")
    GoodsVo findGoodsVoByGoodsId(Long goodsId);
}