package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import com.kirisamemarisa.seckillsystem.mapper.GoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillGoodsMapper;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;
import java.util.List;

@Service
@Slf4j
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {
    @Autowired private GoodsMapper goodsMapper;

    @Autowired private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private RedissonClient redissonClient;

    @PostConstruct
    public void initDoubleDeleteListener() {
        Thread daemonThread = new Thread(() -> {
            RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue("delay_double_delete_queue");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Long goodsId = blockingQueue.take();
                    redisTemplate.delete("seckill:goodsVo:" + goodsId);
                    log.info("【高可用延迟双删】从Redisson延时队列拿到指令，成功完成了商品{}的缓存二次清理", goodsId);
                } catch (InterruptedException e) {
                    log.warn("【高可用延迟双删】线程被中断退出");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (e instanceof org.redisson.RedissonShutdownException || redissonClient.isShutdown()) {
                        log.warn("【高可用延迟双删】监测到 Redisson 客户端已关闭，双删守护线程安全退出。");
                        break;
                    }
                    log.error("【高可用延迟双删】清理异常", e);
                    try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ie) { break; }
                }
            }
        }, "Double-Delete-Thread");
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    @Override
    public List<GoodsVo> findGoodsVo() { return goodsMapper.findGoodsVo(); }

    @Override
    public GoodsVo findGoodsVoByGoodsId(Long goodsId) {
        String cacheKey = "seckill:goodsVo:" + goodsId;
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        if (bloomFilter.isExists() && !bloomFilter.contains(goodsId)) {
            return null;
        }
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            GoodsVo goodsVo = (GoodsVo) cachedObj;
            if (goodsVo.getId() != null && goodsVo.getId().equals(-1L)) {
                return null;
            }
            return goodsVo;
        }
        RLock lock = redissonClient.getLock("seckill:goodsVo:lock:" + goodsId);
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    cachedObj = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedObj != null) {
                        GoodsVo goodsVo = (GoodsVo) cachedObj;
                        if (goodsVo.getId() != null && goodsVo.getId().equals(-1L)) {
                            return null;
                        }
                        return goodsVo;
                    }
                    GoodsVo dbGoodsVo = goodsMapper.findGoodsVoByGoodsId(goodsId);
                    if (dbGoodsVo == null) {
                        GoodsVo emptyObject = new GoodsVo();
                        emptyObject.setId(-1L);
                        redisTemplate.opsForValue().set(cacheKey, emptyObject, 1, TimeUnit.MINUTES);
                        return null;
                    }
                    redisTemplate.opsForValue().set(cacheKey, dbGoodsVo, 60, TimeUnit.MINUTES);
                    return dbGoodsVo;
                } finally {
                    lock.unlock();
                }
            } else {
                Thread.sleep(100);
                return findGoodsVoByGoodsId(goodsId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
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
        stringRedisTemplate.opsForValue().set("seckillGoods:" + newGoodsId, String.valueOf(addGoodsVo.getSeckillStock()));
        stringRedisTemplate.delete("isStockEmpty:" + newGoodsId);
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        if (!bloomFilter.isExists()) { bloomFilter.tryInit(10000L, 0.01); }
        bloomFilter.add(newGoodsId);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + newGoodsId);
        rateLimiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);
        return RespBean.success("商品上架成功！新增ID为：" + newGoodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean deleteSeckillGoods(Long goodsId) {
        goodsMapper.deleteById(goodsId);
        seckillGoodsMapper.delete(new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));
        stringRedisTemplate.delete("seckillGoods:" + goodsId);
        stringRedisTemplate.delete("isStockEmpty:" + goodsId);
        redisTemplate.delete("seckill:goodsVo:" + goodsId);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + goodsId);
        if(rateLimiter.isExists()){ rateLimiter.delete(); }
        return RespBean.success("旧有秒杀商品已彻底下架！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean updateSeckillGoods(UpdateGoodsVo vo) {
        Long goodsId = vo.getId();
        Goods existGoods = goodsMapper.selectById(goodsId);
        if (existGoods == null) { return RespBean.error(RespBeanEnum.BIND_ERROR); }
        redisTemplate.delete("seckill:goodsVo:" + goodsId);
        boolean needUpdateGoods = false;
        Goods goods = new Goods();
        goods.setId(goodsId);
        if (vo.getGoodsName() != null) { goods.setGoodsName(vo.getGoodsName()); needUpdateGoods = true; }
        if (vo.getGoodsTitle() != null) { goods.setGoodsTitle(vo.getGoodsTitle()); needUpdateGoods = true; }
        if (vo.getGoodsImg() != null) { goods.setGoodsImg(vo.getGoodsImg()); needUpdateGoods = true; }
        if (vo.getGoodsDetail() != null) { goods.setGoodsDetail(vo.getGoodsDetail()); needUpdateGoods = true; }
        if (vo.getGoodsPrice() != null) { goods.setGoodsPrice(vo.getGoodsPrice()); needUpdateGoods = true; }
        if (vo.getGoodsStock() != null) { goods.setGoodsStock(vo.getGoodsStock()); needUpdateGoods = true; }
        if (needUpdateGoods) { goodsMapper.updateById(goods); }
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
            stringRedisTemplate.opsForValue().set("seckillGoods:" + goodsId, String.valueOf(vo.getSeckillStock()));
            if (vo.getSeckillStock() > 0) {
                stringRedisTemplate.delete("isStockEmpty:" + goodsId);
                stringRedisTemplate.convertAndSend("stock_replenish_channel", goodsId.toString());
            } else {
                stringRedisTemplate.opsForValue().set("isStockEmpty:" + goodsId, "0");
            }
        }
        RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue("delay_double_delete_queue");
        RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(goodsId, 500, TimeUnit.MILLISECONDS);
        return RespBean.success("商品信息与缓存状态热同步完毕！已投递容灾级延迟双删队列。");
    }

    @Override
    public long countSeckillGoods() {
        return goodsMapper.countSeckillGoods();
    }

    @Override
    public List<GoodsVo> findGoodsVoByLimit(int offset, int size) {
        return goodsMapper.findGoodsVoByLimit(offset, size);
    }
}