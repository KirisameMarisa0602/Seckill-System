package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import com.kirisamemarisa.seckillsystem.exception.GlobalException;
import com.kirisamemarisa.seckillsystem.mapper.GoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.OrderInfoMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillGoodsMapper;
import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * {@link IGoodsService} 实现。主表 {@code t_goods}，秒杀场次 {@code t_seckill_goods}。
 * 详情走「布隆 + Redis + 互斥锁回源」；写操作在事务提交后再改缓存。
 */
@Service
@Slf4j
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {
    @Autowired private GoodsMapper goodsMapper;

    @Autowired private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired private OrderInfoMapper orderInfoMapper;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private RedissonClient redissonClient;

    @Autowired @Qualifier("doubleDeleteExecutor") private Executor doubleDeleteExecutor;

    /**
     * 启动后常驻消费 Redisson 延迟队列：更新商品后 500ms 再删一次详情缓存，减轻「先删缓存再被旧值打回」的窗口。
     * {@code take()} 阻塞当前线程，所以丢到独立线程池，避免卡住 Spring 启动。
     */
    @PostConstruct
    public void initDoubleDeleteListener() {
        doubleDeleteExecutor.execute(() -> {
            RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue("delay_double_delete_queue");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Long goodsId = blockingQueue.take();
                    redisTemplate.delete(GoodsKey.getGoodsVo.getPrefix() + goodsId);
                    log.info("【高可用延迟双删】执行完成商品的缓存二次清理，ID: {}", goodsId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (redissonClient.isShutdown()) break;
                    try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ie) { break; }
                }
            }
        });
    }

    @Override
    public List<GoodsVo> findGoodsVo() { return goodsMapper.findGoodsVo(); }

    @Override
    public GoodsVo findGoodsVoByGoodsId(Long goodsId) {
        String cacheKey = GoodsKey.getGoodsVo.getPrefix() + goodsId;
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        // 布隆说不存在则一定不在集合里，直接返回，避免缓存/DB 被乱 ID 打穿
        if (bloomFilter.isExists() && !bloomFilter.contains(goodsId)) { return null; }
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            GoodsVo goodsVo = (GoodsVo) cachedObj;
            // id=-1 是故意写入的空对象，表示「查过 DB 没有」，TTL 1 分钟
            return (goodsVo.getId() != null && goodsVo.getId().equals(-1L)) ? null : goodsVo;
        }
        RLock lock = redissonClient.getLock("seckill:goodsVo:lock:" + goodsId);
        int retryCount = 0;
        while (retryCount < 3) {
            try {
                if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                    try {
                        // 拿到锁后再读一次，可能已被先行线程回填
                        cachedObj = redisTemplate.opsForValue().get(cacheKey);
                        if (cachedObj != null) {
                            GoodsVo goodsVo = (GoodsVo) cachedObj;
                            return (goodsVo.getId() != null && goodsVo.getId().equals(-1L)) ? null : goodsVo;
                        }
                        GoodsVo dbGoodsVo = goodsMapper.findGoodsVoByGoodsId(goodsId);
                        if (dbGoodsVo == null) {
                            GoodsVo emptyObject = new GoodsVo();
                            emptyObject.setId(-1L);
                            redisTemplate.opsForValue().set(cacheKey, emptyObject, 1, TimeUnit.MINUTES);
                            return null;
                        }
                        redisTemplate.opsForValue().set(cacheKey, dbGoodsVo, GoodsKey.getGoodsVo.expireSeconds(), TimeUnit.SECONDS);
                        return dbGoodsVo;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    retryCount++;
                    Thread.sleep(100);
                    cachedObj = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedObj != null) {
                        GoodsVo goodsVo = (GoodsVo) cachedObj;
                        return (goodsVo.getId() != null && goodsVo.getId().equals(-1L)) ? null : goodsVo;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        throw new GlobalException(RespBeanEnum.RATE_LIMIT_ERROR);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean addSeckillGoods(AddGoodsVo addGoodsVo) {
        RespBean validationError = validateInventoryAndPrice(
                addGoodsVo.getGoodsStock(),
                addGoodsVo.getSeckillStock(),
                addGoodsVo.getGoodsPrice(),
                addGoodsVo.getSeckillPrice()
        );
        if (validationError != null) {
            return validationError;
        }
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
        afterCommit(() -> {
            stringRedisTemplate.opsForValue().set(
                    GoodsKey.getSeckillGoodsStock.getPrefix() + newGoodsId,
                    String.valueOf(addGoodsVo.getSeckillStock()));
            stringRedisTemplate.delete(GoodsKey.isStockEmpty.getPrefix() + newGoodsId);
            RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
            if (!bloomFilter.isExists()) { bloomFilter.tryInit(10000L, 0.01); }
            bloomFilter.add(newGoodsId);
            RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + newGoodsId);
            rateLimiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);
        });
        return RespBean.success("商品上架成功！新增ID为：" + newGoodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean deleteSeckillGoods(Long goodsId) {
        if (countPendingOrders(goodsId) > 0) {
            return businessError("该商品仍有待支付订单，禁止下架");
        }
        goodsMapper.deleteById(goodsId);
        seckillGoodsMapper.delete(new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));
        afterCommit(() -> {
            stringRedisTemplate.delete(GoodsKey.getSeckillGoodsStock.getPrefix() + goodsId);
            stringRedisTemplate.delete(GoodsKey.isStockEmpty.getPrefix() + goodsId);
            redisTemplate.delete(GoodsKey.getGoodsVo.getPrefix() + goodsId);
            RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + goodsId);
            if (rateLimiter.isExists()) { rateLimiter.delete(); }
        });
        return RespBean.success("旧有秒杀商品已彻底下架！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RespBean updateSeckillGoods(UpdateGoodsVo vo) {
        Long goodsId = vo.getId();
        Goods existGoods = goodsMapper.selectById(goodsId);
        if (existGoods == null) { return RespBean.error(RespBeanEnum.BIND_ERROR); }
        GoodsVo current = goodsMapper.findGoodsVoByGoodsId(goodsId);
        if (current == null || current.getStockCount() == null) {
            return businessError("秒杀商品配置不存在");
        }

        Integer effectiveGoodsStock = vo.getGoodsStock() == null ? existGoods.getGoodsStock() : vo.getGoodsStock();
        Integer effectiveSeckillStock = vo.getSeckillStock() == null ? current.getStockCount() : vo.getSeckillStock();
        java.math.BigDecimal effectiveGoodsPrice =
                vo.getGoodsPrice() == null ? existGoods.getGoodsPrice() : vo.getGoodsPrice();
        java.math.BigDecimal effectiveSeckillPrice =
                vo.getSeckillPrice() == null ? current.getSeckillPrice() : vo.getSeckillPrice();
        RespBean validationError = validateInventoryAndPrice(
                effectiveGoodsStock, effectiveSeckillStock, effectiveGoodsPrice, effectiveSeckillPrice);
        if (validationError != null) {
            return validationError;
        }

        LocalDateTime effectiveStart = vo.getStartDate() == null ? current.getStartDate() : vo.getStartDate();
        LocalDateTime effectiveEnd = vo.getEndDate() == null ? current.getEndDate() : vo.getEndDate();
        if (effectiveStart == null || effectiveEnd == null || !effectiveEnd.isAfter(effectiveStart)) {
            return businessError("秒杀结束时间必须晚于开始时间");
        }
        boolean inventoryChanged = vo.getGoodsStock() != null || vo.getSeckillStock() != null;
        boolean activityRunning = current.getStartDate() != null && current.getEndDate() != null
                && !LocalDateTime.now().isBefore(current.getStartDate())
                && !LocalDateTime.now().isAfter(current.getEndDate());
        // 进行中直接覆盖库存会和 Redis 预扣、待支付单对不上
        if (inventoryChanged && (activityRunning || countPendingOrders(goodsId) > 0)) {
            return businessError("活动进行中或仍有待支付订单，禁止直接覆盖库存");
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
        afterCommit(() -> {
            redisTemplate.delete(GoodsKey.getGoodsVo.getPrefix() + goodsId);
            if (vo.getSeckillStock() != null) {
                stringRedisTemplate.opsForValue().set(
                        GoodsKey.getSeckillGoodsStock.getPrefix() + goodsId,
                        String.valueOf(vo.getSeckillStock()));
                if (vo.getSeckillStock() > 0) {
                    stringRedisTemplate.delete(GoodsKey.isStockEmpty.getPrefix() + goodsId);
                    stringRedisTemplate.convertAndSend("stock_replenish_channel", goodsId.toString());
                } else {
                    stringRedisTemplate.opsForValue().set(
                            GoodsKey.isStockEmpty.getPrefix() + goodsId, "1");
                }
            }
            RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue("delay_double_delete_queue");
            RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
            delayedQueue.offer(goodsId, 500, TimeUnit.MILLISECONDS);
        });
        return RespBean.success("商品信息与缓存状态热同步完毕！已投递容灾级延迟双删队列。");
    }

    /** 待支付单数量。status=0 表示还占着秒杀库存预扣，不能下架或覆盖库存。 */
    private long countPendingOrders(Long goodsId) {
        return orderInfoMapper.selectCount(new QueryWrapper<OrderInfo>()
                .eq("goods_id", goodsId)
                .eq("status", 0));
    }

    /** 秒杀库存不能大于普通库存，秒杀价不能高于原价；违规返回 BIND_ERROR。 */
    private RespBean validateInventoryAndPrice(Integer goodsStock, Integer seckillStock,
                                               java.math.BigDecimal goodsPrice,
                                               java.math.BigDecimal seckillPrice) {
        if (goodsStock == null || seckillStock == null || seckillStock > goodsStock) {
            return businessError("秒杀库存不能大于普通库存");
        }
        if (goodsPrice == null || seckillPrice == null || seckillPrice.compareTo(goodsPrice) > 0) {
            return businessError("秒杀价格不能高于商品原价");
        }
        return null;
    }

    /** 复用 BIND_ERROR 的 code，只改 message 给前端展示具体原因。 */
    private RespBean businessError(String message) {
        RespBean response = RespBean.error(RespBeanEnum.BIND_ERROR);
        response.setMessage(message);
        return response;
    }

    /**
     * 事务提交后再跑缓存同步。回滚时不会执行；无事务时（例如单测直接调）立刻跑。
     * 缓存失败只打日志，DB 已提交，靠预热/对账恢复。
     */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (Exception e) {
                    log.error("数据库事务已提交，但缓存同步失败，将由预热/对账任务恢复", e);
                }
            }
        });
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
