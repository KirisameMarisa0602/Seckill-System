package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheWarmUpRunner implements ApplicationRunner {
    @Autowired private IGoodsService goodsService;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private RedissonClient redissonClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("====== 🚀 正在启动秒杀预热引擎，重载防线数据 ======");
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        bloomFilter.delete();
        bloomFilter.tryInit(10000L, 0.01);
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
                stringRedisTemplate.opsForValue().set(GoodsKey.getSeckillGoodsStock.getPrefix() + gId, String.valueOf(goods.getStockCount()));
                if (goods.getStockCount() > 0) {
                    stringRedisTemplate.delete(GoodsKey.isStockEmpty.getPrefix() + gId);
                } else {
                    stringRedisTemplate.opsForValue().set(GoodsKey.isStockEmpty.getPrefix() + gId, "0");
                }
                RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + gId);
                rateLimiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);
            }
        }
        log.info("====== ✨ 预热引擎满载完毕，布隆过滤器、令牌机制完成战备状态！ ======");
    }
}