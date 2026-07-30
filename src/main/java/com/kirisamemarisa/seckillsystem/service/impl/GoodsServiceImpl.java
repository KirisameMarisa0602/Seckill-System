package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import com.kirisamemarisa.seckillsystem.mapper.GoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillGoodsMapper;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.*;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public List<GoodsVo> findGoodsVo() {
        return goodsMapper.findGoodsVo();
    }

    @Override
    public GoodsVo findGoodsVoByGoodsId(Long goodsId) {
        return goodsMapper.findGoodsVoByGoodsId(goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean addSeckillGoods(AddGoodsVo addGoodsVo) {
        Goods goods = new Goods();
        goods.setGoodsName(addGoodsVo.getGoodsName());
        goods.setGoodsTitle(addGoodsVo.getGoodsTitle());
        goods.setGoodsImg(addGoodsVo.getGoodsImg());
        goods.setGoodsDetail(addGoodsVo.getGoodsDetail());
        goods.setGoodsPrice(addGoodsVo.getGoodsPrice());
        goods.setGoodsStock(addGoodsVo.getGoodsStock());
        goodsMapper.insert(goods);

        Long newGoodsId = goods.getId();

        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setGoodsId(newGoodsId);
        seckillGoods.setSeckillPrice(addGoodsVo.getSeckillPrice());
        seckillGoods.setStockCount(addGoodsVo.getSeckillStock());
        seckillGoods.setStartDate(addGoodsVo.getStartDate());
        seckillGoods.setEndDate(addGoodsVo.getEndDate());
        seckillGoodsMapper.insert(seckillGoods);

        // 上架必做：预热Redis与注册布隆过滤器
        redisTemplate.opsForValue().set("seckillGoods:" + newGoodsId, addGoodsVo.getSeckillStock());
        redisTemplate.delete("isStockEmpty:" + newGoodsId);

        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        bloomFilter.add(newGoodsId);

        return RespBean.success("商品上架成功！新增ID为：" + newGoodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean deleteSeckillGoods(Long goodsId) {
        // 1. 强力清除数据库双表中的商品数据
        goodsMapper.deleteById(goodsId);
        seckillGoodsMapper.delete(new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));

        // 2. 清理 Redis 中的秒杀库存标识和售罄标识
        redisTemplate.delete("seckillGoods:" + goodsId);
        redisTemplate.delete("isStockEmpty:" + goodsId);

        // 注: 布隆过滤器的标准实现中不提供元素的删除，所以直接无视它即可。
        // 即便黑客拿着已删除的 goodsId 通过了布隆过滤器，当走到获取 Redis 缓存那一步时也取不到库存
        // 你的底层 Lua 脚本依然会直接返回库存不足，完美拒之门外！

        return RespBean.success("旧有秒杀商品已彻底下架！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean updateSeckillGoods(UpdateGoodsVo vo) {
        Long goodsId = vo.getId();
        // 0. 防御性检查
        Goods existGoods = goodsMapper.selectById(goodsId);
        if (existGoods == null) {
            return RespBean.error(RespBeanEnum.BIND_ERROR);
        }

        // 1. 局部更新：商品主表 (t_goods)
        boolean needUpdateGoods = false;
        Goods goods = new Goods();
        goods.setId(goodsId);
        if (vo.getGoodsName() != null) { goods.setGoodsName(vo.getGoodsName()); needUpdateGoods = true; }
        if (vo.getGoodsTitle() != null) { goods.setGoodsTitle(vo.getGoodsTitle()); needUpdateGoods = true; }
        if (vo.getGoodsImg() != null) { goods.setGoodsImg(vo.getGoodsImg()); needUpdateGoods = true; }
        if (vo.getGoodsDetail() != null) { goods.setGoodsDetail(vo.getGoodsDetail()); needUpdateGoods = true; }
        if (vo.getGoodsPrice() != null) { goods.setGoodsPrice(vo.getGoodsPrice()); needUpdateGoods = true; }
        if (vo.getGoodsStock() != null) { goods.setGoodsStock(vo.getGoodsStock()); needUpdateGoods = true; }

        // 只有在主表确实有数据要改时，才执行操作避免生成空SQL报错
        if (needUpdateGoods) {
            goodsMapper.updateById(goods);
        }

        // 2. 局部更新：秒杀副表 (t_seckill_goods)
        boolean needUpdateSeckill = false;
        SeckillGoods sg = new SeckillGoods();
        if (vo.getSeckillPrice() != null) { sg.setSeckillPrice(vo.getSeckillPrice()); needUpdateSeckill = true; }
        if (vo.getSeckillStock() != null) { sg.setStockCount(vo.getSeckillStock()); needUpdateSeckill = true; }
        if (vo.getStartDate() != null) { sg.setStartDate(vo.getStartDate()); needUpdateSeckill = true; }
        if (vo.getEndDate() != null) { sg.setEndDate(vo.getEndDate()); needUpdateSeckill = true; }

        // 只有确实修改了秒杀信息，才去操作副表
        if (needUpdateSeckill) {
            seckillGoodsMapper.update(sg, new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));
        }

        // 3. 【Redis 状态同步热更新】
        if (vo.getSeckillStock() != null) {
            // (1) 强制覆盖 Redis 最新的秒杀剩余库存
            redisTemplate.opsForValue().set("seckillGoods:" + goodsId, vo.getSeckillStock());

            // (2) 智能起死回生
            if (vo.getSeckillStock() > 0) {
                redisTemplate.delete("isStockEmpty:" + goodsId);
                redisTemplate.convertAndSend("stock_replenish_channel", goodsId.toString());
            } else {
                redisTemplate.opsForValue().set("isStockEmpty:" + goodsId, "0");
            }
        }

        return RespBean.success("商品信息与缓存状态热同步完毕！");
    }
}