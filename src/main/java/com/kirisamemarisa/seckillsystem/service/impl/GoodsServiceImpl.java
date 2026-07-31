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
        redisTemplate.opsForValue().set("seckillGoods:" + newGoodsId, addGoodsVo.getSeckillStock());
        redisTemplate.delete("isStockEmpty:" + newGoodsId);
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(10000L, 0.01);
        }
        bloomFilter.add(newGoodsId);
        return RespBean.success("商品上架成功！新增ID为：" + newGoodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean deleteSeckillGoods(Long goodsId) {
        goodsMapper.deleteById(goodsId);
        seckillGoodsMapper.delete(new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));
        redisTemplate.delete("seckillGoods:" + goodsId);
        redisTemplate.delete("isStockEmpty:" + goodsId);
        return RespBean.success("旧有秒杀商品已彻底下架！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean updateSeckillGoods(UpdateGoodsVo vo) {
        Long goodsId = vo.getId();
        Goods existGoods = goodsMapper.selectById(goodsId);
        if (existGoods == null) {
            return RespBean.error(RespBeanEnum.BIND_ERROR);
        }
        boolean needUpdateGoods = false;
        Goods goods = new Goods();
        goods.setId(goodsId);
        if (vo.getGoodsName() != null) { goods.setGoodsName(vo.getGoodsName()); needUpdateGoods = true; }
        if (vo.getGoodsTitle() != null) { goods.setGoodsTitle(vo.getGoodsTitle()); needUpdateGoods = true; }
        if (vo.getGoodsImg() != null) { goods.setGoodsImg(vo.getGoodsImg()); needUpdateGoods = true; }
        if (vo.getGoodsDetail() != null) { goods.setGoodsDetail(vo.getGoodsDetail()); needUpdateGoods = true; }
        if (vo.getGoodsPrice() != null) { goods.setGoodsPrice(vo.getGoodsPrice()); needUpdateGoods = true; }
        if (vo.getGoodsStock() != null) { goods.setGoodsStock(vo.getGoodsStock()); needUpdateGoods = true; }
        if (needUpdateGoods) {
            goodsMapper.updateById(goods);
        }
        boolean needUpdateSeckill = false;
        SeckillGoods sg = new SeckillGoods();
        if (vo.getSeckillPrice() != null) { sg.setSeckillPrice(vo.getSeckillPrice()); needUpdateSeckill = true; }
        if (vo.getSeckillStock() != null) { sg.setStockCount(vo.getSeckillStock()); needUpdateSeckill = true; }
        if (vo.getStartDate() != null) { sg.setStartDate(vo.getStartDate()); needUpdateSeckill = true; }
        if (vo.getEndDate() != null) { sg.setEndDate(vo.getEndDate()); needUpdateSeckill = true; }
        if (needUpdateSeckill) {
            seckillGoodsMapper.update(sg, new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));
        }
        if (vo.getSeckillStock() != null) {
            redisTemplate.opsForValue().set("seckillGoods:" + goodsId, vo.getSeckillStock());
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