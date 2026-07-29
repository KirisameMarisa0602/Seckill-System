package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @RequestMapping(value = "/warmup", method = RequestMethod.POST)
    @ResponseBody
    public RespBean cacheWarmUp() {
        List<GoodsVo> goodsList = goodsService.findGoodsVo();
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        bloomFilter.tryInit(10000L, 0.01);
        if (goodsList == null || goodsList.isEmpty()) {
            return RespBean.error(null);
        }
        System.out.println("==============================================================");
        System.out.println("======== 【运营后台触发：秒杀商品缓存预热 / 库存重置】 ========");
        System.out.println("==============================================================");
        for (GoodsVo goods : goodsList) {
            bloomFilter.add(goods.getId());
            redisTemplate.opsForValue().set("seckillGoods:" + goods.getId(), goods.getStockCount());
            if (goods.getStockCount() > 0) {
                redisTemplate.delete("isStockEmpty:" + goods.getId());
            } else {
                redisTemplate.opsForValue().set("isStockEmpty:" + goods.getId(), "0");
            }
            System.out.printf(" >> 管理员加载商品 | ID: %-2d | 名称: %-15s | 重新注入 Redis 库存: %d 份 \n",
                    goods.getId(), goods.getGoodsName(), goods.getStockCount());
        }
        System.out.println("==============================================================");
        return RespBean.success("缓存预热/重置成功，请查看控制台日志！");
    }
}