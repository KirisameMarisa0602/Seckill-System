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
    private IAdminService adminService; // 注入真正的管理员鉴权服务
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private IUserService userService;   // 注入用户服务用于管理用户

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 1. 运营人员登录 (真正的数据库查询验证)
     */
    @PostMapping("/login")
    @ResponseBody
    public RespBean login(@Valid @RequestBody AdminLoginVo vo) {
        // 全权交给 Service 中的 MybatisPlus 去查库比对密码并颁发令牌
        return adminService.login(vo);
    }

    /**
     * ========================【 用户管理区 】========================
     */

    /**
     * 2. B端查询所有注册用户
     */
    @GetMapping("/user/list")
    @ResponseBody
    public RespBean getUserList() {
        // MybatisPlus自带的 list() 方法直接查出全表！
        List<User> userList = userService.list();
        return RespBean.success(userList);
    }

    /**
     * ========================【 商品管理区 】========================
     */

    /**
     * 3. B端查询全部秒杀商品列表 (含售价库存等所有细节)
     */
    @GetMapping("/goods/list")
    @ResponseBody
    public RespBean getGoodsList() {
        List<GoodsVo> goodsList = goodsService.findGoodsVo();
        return RespBean.success(goodsList);
    }

    /**
     * 4. B端后台上架秒杀商品 (同时写入数据库及 Redis)
     */
    @PostMapping("/goods/add")
    @ResponseBody
    public RespBean addSeckillGoods(@Valid @RequestBody AddGoodsVo addGoodsVo) {
        return goodsService.addSeckillGoods(addGoodsVo);
    }

    /**
     * 5. B端强制下架商品 (完全清理数据库和Redis对应键值)
     */
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
    /**
     * 6. [运维接口] 全量预热/重置环境
     */
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