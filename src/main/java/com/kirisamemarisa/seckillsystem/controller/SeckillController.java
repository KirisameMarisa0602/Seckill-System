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
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private DefaultRedisScript<Long> seckillScript;

    /**
     * V2.0: 全面 Redis 缓存化，实现入口 0 DB 访问
     */
    @PostMapping("/doSeckillV20")
    public RespBean doSeckillV20(Long userId, Long goodsId) {
        if (userId == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }

        // ==========================================
        // 核心改造 1：拦截查库判重！先查 Redis 中是否已有该用户的秒杀成功标记
        // ==========================================
        // KEY的设计规范：系统标识:模块:商品ID:用户ID (例 seckill:order:1:18888888888)
        String orderKey = "seckill:order:" + goodsId + ":" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(orderKey))) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }

        // ==========================================
        // 核心改造 2：不再直接查 DB 拿信息，加入旁路缓存机制 (Cache-Aside)
        // ==========================================
        // 2.1 缓存验证用户
        User user = (User) redisTemplate.opsForValue().get("user:" + userId);
        if (user == null) {
            user = userMapper.selectById(userId);
            if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
            // 写回 Redis，并设置过期时间（模拟登录态 Token 时效）
            redisTemplate.opsForValue().set("user:" + userId, user, 30, TimeUnit.MINUTES);
        }

        // 2.2 缓存验证商品
        GoodsVo goods = (GoodsVo) redisTemplate.opsForValue().get("goodsVo:" + goodsId);
        if (goods == null) {
            goods = goodsService.findGoodsVoByGoodsId(goodsId);
            if (goods == null) { return RespBean.error(RespBeanEnum.EMPTY_STOCK); }
            // 秒杀商品信息通常不会变动，缓存 1 分钟或直到活动结束
            redisTemplate.opsForValue().set("goodsVo:" + goodsId, goods, 1, TimeUnit.MINUTES);
        }

        // ==========================================
        // 核心改造 3（已完成）：原子扣减库存
        // ==========================================
        String stockKey = "seckill:stock:" + goodsId;
        Long res = redisTemplate.execute(seckillScript, Collections.singletonList(stockKey));

        if (res == null || res == 0L) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // ==========================================
        // 核心改造 4：前置锁定防重拦截，记录该用户已经抢购过了！
        // ==========================================
        // 走到这说明秒杀Lua通过了，立马把用户ID刻在 Redis 里拦截他后续可能发狂点的重复请求
        // 设置 15 分钟存活（对应由于后续死信队列会处理未支付订单的时间）
        redisTemplate.opsForValue().set(orderKey, "1", 15, TimeUnit.MINUTES);

        try {
            // 当前这行还会同步查一次 MYSQL (生成订单的写操作)
            // 别急，这就是我们下一个 PR 的大招：RabbitMQ 取代它！
            orderService.createSeckillOrder(user, goods);
            return RespBean.success("秒杀成功！");
        } catch (Exception e) {
            // ⚠️ 极小概率异常兜底：如果在写 DB 环节发生系统异常（比如MySQL刚好断连）
            // 需要回滚 Redis 里的标识，避免用户永久错失购买机会（真实大厂补偿逻辑）
            redisTemplate.delete(orderKey);
            return RespBean.error(RespBeanEnum.ERROR);
        }
    }
}