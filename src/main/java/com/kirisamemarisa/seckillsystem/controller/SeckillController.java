package com.kirisamemarisa.seckillsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.mapper.UserMapper;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * V1.0: Redis Lua 脚本防御超卖版
     * 压测准备参数：/seckill/doSeckillV10?userId=xxx&goodsId=1
     */
    @PostMapping("/doSeckillV10")
    public RespBean doSeckillV10(Long userId, Long goodsId) {

        // 这一步终于不再是摆设了，如果是错的 userId 就会被拦下来
        if (userId == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
        User user = userMapper.selectById(userId);
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }

        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);

        // ==========================================
        // 核心改造 1：将判断库存、扣减库存彻底移交 Redis Lua
        // ==========================================
        String stockKey = "seckill:stock:" + goodsId;
        String luaScript =
                "if (redis.call('exists', KEYS[1]) == 1) then " +
                        "    local stock = tonumber(redis.call('get', KEYS[1])); " +
                        "    if (stock > 0) then " +
                        "        redis.call('decr', KEYS[1]); " +
                        "        return 1; " +
                        "    end; " +
                        "end; " +
                        "return 0;";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        // 执行原子扣减
        Long res = redisTemplate.execute(script, Collections.singletonList(stockKey));

        if (res == null || res == 0L) {
            // 直接由 Redis 宣判死刑，挡死千万并发！
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // ==========================================
        // 核心改造 2：能走到这里的请求，说明他在 Redis 抢过关了！
        // 如果库存只有10，全天下只有 10 个线程能走到这里。
        // ==========================================

        // 简单拦截一下是否同一用户重复点击（通过DB约束拦截）
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>()
                .eq("user_id", user.getId())
                .eq("goods_id", goodsId));
        if (seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }

        try {
            // 放行，让这极少数的幸运儿去排队下订单
            orderService.createSeckillOrder(user, goods);
            return RespBean.success("秒杀成功！");
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR);
        }
    }
}