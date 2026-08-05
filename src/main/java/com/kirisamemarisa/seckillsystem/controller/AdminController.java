package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import com.kirisamemarisa.seckillsystem.service.IAdminService;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.vo.*;
import jakarta.validation.Valid;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired private IAdminService adminService;

    @Autowired private IGoodsService goodsService;

    @Autowired private IUserService userService;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private RedissonClient redissonClient;

    @AccessLimit(second = 60, maxCount = 5, needLogin = false)
    @PostMapping("/login")
    @ResponseBody
    public RespBean login(@Valid @RequestBody AdminLoginVo vo) {
        return adminService.login(vo);
    }

    @GetMapping("/user/list")
    @ResponseBody
    public RespBean getUserList() {
        return RespBean.success(userService.list());
    }

    @GetMapping("/goods/list")
    @ResponseBody
    public RespBean getGoodsList() {
        return RespBean.success(goodsService.findGoodsVo());
    }

    @PostMapping("/goods/add")
    @ResponseBody
    public RespBean addSeckillGoods(@Valid @RequestBody AddGoodsVo addGoodsVo) {
        return goodsService.addSeckillGoods(addGoodsVo);
    }

    @PostMapping("/goods/delete/{goodsId}")
    @ResponseBody
    public RespBean deleteGoods(@PathVariable Long goodsId) {
        return goodsService.deleteSeckillGoods(goodsId);
    }

    @PostMapping("/goods/update")
    @ResponseBody
    public RespBean updateGoods(@Valid @RequestBody UpdateGoodsVo updateGoodsVo) {
        return goodsService.updateSeckillGoods(updateGoodsVo);
    }

    @PostMapping("/warmup")
    @ResponseBody
    public RespBean cacheWarmUp() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        bloomFilter.delete();
        bloomFilter.tryInit(10000L, 0.01);
        long total = goodsService.countSeckillGoods();
        if (total == 0) {
            return RespBean.error(null);
        }
        int pageSize = 1000;
        int totalPages = (int) Math.ceil((double) total / pageSize);
        for (int i = 0; i < totalPages; i++) {
            int offset = i * pageSize;
            List<GoodsVo> list = goodsService.findGoodsVoByLimit(offset, pageSize);
            for (GoodsVo goods : list) {
                bloomFilter.add(goods.getId());
                redisTemplate.opsForValue().set("seckill:goodsVo:" + goods.getId(), goods, 60, TimeUnit.MINUTES);
                stringRedisTemplate.opsForValue().set("seckillGoods:" + goods.getId(), String.valueOf(goods.getStockCount()));
                if (goods.getStockCount() > 0) {
                    stringRedisTemplate.delete("isStockEmpty:" + goods.getId());
                } else {
                    stringRedisTemplate.opsForValue().set("isStockEmpty:" + goods.getId(), "0");
                }
                RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + goods.getId());
                rateLimiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);
            }
        }
        return RespBean.success("灾备重置与预热：已采用分批加载策略，无 OOM 风险，环境完美恢复！");
    }
}