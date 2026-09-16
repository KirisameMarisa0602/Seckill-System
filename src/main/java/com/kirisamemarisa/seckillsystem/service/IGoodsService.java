package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.AddGoodsVo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.UpdateGoodsVo;

import java.util.List;

/**
 * 商品与秒杀场次业务。操作 {@code t_goods} + {@code t_seckill_goods}，
 * 由 {@link com.kirisamemarisa.seckillsystem.service.impl.GoodsServiceImpl} 实现。
 * 写路径在事务提交后再同步 Redis / 布隆过滤器，避免回滚后缓存脏数据。
 */
public interface IGoodsService extends IService<Goods> {
    /**
     * 联表查出全部秒杀商品视图，供后台/列表一次性拉取。无事务。
     */
    List<GoodsVo> findGoodsVo();

    /**
     * 按商品 ID 查秒杀视图：布隆过滤器 → Redis → 互斥锁回源 DB。
     *
     * <p>确定不存在返回 {@code null}（缓存里用 {@code id=-1} 的空对象占位防穿透）。
     * 抢锁重试耗尽抛 {@code GlobalException(RATE_LIMIT_ERROR)}，由全局异常处理成 JSON。
     */
    GoodsVo findGoodsVoByGoodsId(Long goodsId);

    /**
     * 管理员上架：同一事务内插入 {@code t_goods} 与 {@code t_seckill_goods}。
     *
     * <p>秒杀库存大于普通库存、秒杀价高于原价时返回 {@code BIND_ERROR}（message 覆盖为具体原因）。
     * 提交后再写 Redis 库存、布隆过滤器与限流器。
     */
    RespBean addSeckillGoods(AddGoodsVo addGoodsVo);

    /**
     * 管理员下架。仍有待支付订单（{@code t_order.status=0}）时拒绝，返回 {@code BIND_ERROR}。
     * 事务内删两张表；提交后清 Redis 库存、详情缓存和限流器。
     */
    RespBean deleteSeckillGoods(Long goodsId);

    /**
     * 管理员热更新。商品不存在返回 {@code BIND_ERROR}；活动进行中或仍有待支付单时禁止改库存。
     * 只更新入参非空字段。提交后删详情缓存，必要时覆盖 Redis 可售库存并投递延迟双删。
     */
    RespBean updateSeckillGoods(UpdateGoodsVo updateGoodsVo);

    /**
     * 已配置秒杀场次的商品总数，给后台分页用。无事务。
     */
    long countSeckillGoods();

    /**
     * 联表分页查询秒杀商品。{@code offset}/{@code size} 由调用方算好，对应 SQL {@code LIMIT offset, size}。
     */
    List<GoodsVo> findGoodsVoByLimit(int offset, int size);
}
