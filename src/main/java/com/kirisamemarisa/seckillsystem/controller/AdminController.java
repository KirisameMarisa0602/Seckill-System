package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台管理控制台 (Admin Console)
 * 供 B端(运营/管理员) 调用的接口，与 C端(高并发用户) 物理逻辑隔离
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 【一键预热】管理员手动触发缓存预热与库存重置
     *  作用：应用启动后，或中途临时加库存，只需调用此接口即可刷新 Redis 数据。
     */
    @RequestMapping(value = "/warmup", method = RequestMethod.POST)
    @ResponseBody
    public RespBean cacheWarmUp() {
        List<GoodsVo> goodsList = goodsService.findGoodsVo();
        if (goodsList == null || goodsList.isEmpty()) {
            return RespBean.error(null); // 可自定义个枚举："暂无秒杀商品"
        }

        System.out.println("==============================================================");
        System.out.println("======== 【运营后台触发：秒杀商品缓存预热 / 库存重置】 ========");
        System.out.println("==============================================================");

        for (GoodsVo goods : goodsList) {
            // 1. 将真实的 MySQL 秒杀库存写入 Redis
            redisTemplate.opsForValue().set("seckillGoods:" + goods.getId(), goods.getStockCount());

            // 2. 清理或者重置“库存空”标记
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