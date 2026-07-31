package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.service.IAdminService;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.vo.*;
import jakarta.validation.Valid;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private IAdminService adminService;

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private IUserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @PostMapping("/login")
    @ResponseBody
    public RespBean login(@Valid @RequestBody AdminLoginVo vo) {
        return adminService.login(vo);
    }

    @GetMapping("/user/list")
    @ResponseBody
    public RespBean getUserList() {
        List<User> userList = userService.list();
        return RespBean.success(userList);
    }

    @GetMapping("/goods/list")
    @ResponseBody
    public RespBean getGoodsList() {
        List<GoodsVo> goodsList = goodsService.findGoodsVo();
        return RespBean.success(goodsList);
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
        List<GoodsVo> goodsList = goodsService.findGoodsVo();
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        bloomFilter.tryInit(10000L, 0.01);
        if (goodsList == null || goodsList.isEmpty()) {
            return RespBean.error(null);
        }
        for (GoodsVo goods : goodsList) {
            bloomFilter.add(goods.getId());
            redisTemplate.opsForValue().set("seckillGoods:" + goods.getId(), goods.getStockCount());
            if (goods.getStockCount() > 0) {
                redisTemplate.delete("isStockEmpty:" + goods.getId());
            } else {
                redisTemplate.opsForValue().set("isStockEmpty:" + goods.getId(), "0");
            }
        }
        return RespBean.success("灾备重置：缓存环境已经依照数据库当前状况完美恢复！");
    }
}