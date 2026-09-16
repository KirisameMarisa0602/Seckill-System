package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
import com.kirisamemarisa.seckillsystem.manager.LocalCacheManager;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(20)
public class CacheWarmUpRunner implements ApplicationRunner {
    @Autowired private IGoodsService goodsService;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private RedissonClient redissonClient;

    @Autowired private LocalCacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("正在安全预热秒杀只读缓存；不会覆盖运行中的 Redis 可售库存");
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(10000L, 0.01);
        }
        long total = goodsService.countSeckillGoods();
        if (total == 0) {
            log.info("=> 发现库内当前无秒杀商品预热。");
            return;
        }
        int pageSize = 1000;
        int totalPages = (int) Math.ceil((double) total / pageSize);
        for (int i = 0; i < totalPages; i++) {
            int offset = i * pageSize;
            List<GoodsVo> list = goodsService.findGoodsVoByLimit(offset, pageSize);
            for (GoodsVo goods : list) {
                Long gId = goods.getId();
                bloomFilter.add(gId);
                redisTemplate.opsForValue().set(GoodsKey.getGoodsVo.getPrefix() + gId, goods, GoodsKey.getGoodsVo.expireSeconds(), TimeUnit.SECONDS);
                if (goods.getStockCount() == null) {
                    log.warn("商品 {} 没有秒杀库存配置，已跳过库存预热", gId);
                    continue;
                }
                String stockKey = GoodsKey.getSeckillGoodsStock.getPrefix() + gId;
                Boolean initialized = stringRedisTemplate.opsForValue().setIfAbsent(
                        stockKey, String.valueOf(goods.getStockCount()));
                if (Boolean.TRUE.equals(initialized)) {
                    if (goods.getStockCount() > 0) {
                        stringRedisTemplate.delete(GoodsKey.isStockEmpty.getPrefix() + gId);
                    } else {
                        stringRedisTemplate.opsForValue().set(GoodsKey.isStockEmpty.getPrefix() + gId, "1");
                    }
                }
                if (goods.getStockCount() > 0) {
                    cacheManager.removeEmpty(gId);
                    stringRedisTemplate.convertAndSend("stock_replenish_channel", gId.toString());
                }
                RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + gId);
                rateLimiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);
            }
        }
        log.info("安全预热完成");
    }
}