package com.kirisamemarisa.seckillsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.rabbitmq.MQSender;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.InitializingBean;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MQSender mqSender;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private IGoodsService goodsService;

    // 【核心修复 1】：注入在 RedisConfig 中已经配置好的 Lua 脚本 Bean
    @Autowired
    private DefaultRedisScript<Long> seckillScript;

    /**
     * V3.0 异步终极版：秒杀接口 (接入 Lua 脚本保证绝对原子性)
     */
    @RequestMapping(value = "/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(User user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }

        // 【核心修复 2】：使用 Lua 脚本进行原子扣减，彻底杜绝并发下的超卖/少卖
        // 执行 Lua 脚本，传入 KEYS[1] 即秒杀商品的 Redis Key
        Long result = (Long) redisTemplate.execute(
                seckillScript,
                Collections.singletonList("seckillGoods:" + goodsId)
        );

        // 根据 Lua 脚本的返回值判断（我们在脚本里定好的：1代表成功扣减，0代表库存不足/不存在）
        if (result == null || result == 0L) {
            // 🔥给这个商品打上一个“售罄”的红叉标记，存入Redis (很重要，下面轮询要用)
            redisTemplate.opsForValue().set("isStockEmpty:" + goodsId, "0");
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 2. 拿到资格，扔小票（封装秒杀消息）
        SeckillMessage message = new SeckillMessage(user, goodsId);

        // 3. 把小票往 RabbitMQ (缓冲分发屏) 里一扔！
        mqSender.sendSeckillMessage(message);

        // 4. 返回 0 告诉前端：【排队中】
        return RespBean.success(0);
    }

    /**
     * V3.0 新增：客户端轮询接口
     * 前端会每隔 1 两秒悄悄调用这个接口
     * 返回值定义： orderId(抢购成功) ; -1 (库存不足没抢到) ; 0 (还在排队努力中)
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }

        // 1. 先去抢购订单表里查一下，后厨机器人有没有把我的单子做出来
        // TODO: 这里目前直接查 MySQL ，高并发下会导致数据库崩溃，将在下一个 Commit 中修复！
        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).eq("goods_id", goodsId);
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(wrapper);

        if (seckillOrder != null) {
            // 恭喜！后厨已经帮你做好了，直接返回确切的订单ID，前端拿到后跳收银台页面！
            return RespBean.success(seckillOrder.getOrderId());
        }

        // 2. 如果没查到订单，那是还在排队，还是已经卖光了？去 Redis 看看有没有售罄标记
        boolean isStockEmpty = redisTemplate.hasKey("isStockEmpty:" + goodsId);

        if (isStockEmpty) {
            // 惨，前台已经挂“售罄”牌子了，你没戏了
            return RespBean.success(-1);
        }

        // 3. 没查到订单，也没卖光（还在做），那就是正在处理中，耐心等
        return RespBean.success(0);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.findGoodsVo();
        if (goodsList == null) {
            return;
        }

        System.out.println("==============================================================");
        System.out.println("======== \uD83D\uDE80 正在执行系统级别【缓存预热 Cache Warm-up】 ========");
        System.out.println("==============================================================");

        for (GoodsVo goods : goodsList) {
            // 将真实库存写入 Redis
            redisTemplate.opsForValue().set("seckillGoods:" + goods.getId(), goods.getStockCount());
            // 清除上次运行遗留的售罄标记
            redisTemplate.delete("isStockEmpty:" + goods.getId());

            // 打印出一条极具观赏性的日志
            System.out.printf(" \uD83D\uDCE6 加载商品 | ID: %-2d | 名称: %-15s | 注入 Redis 秒杀库存数: %d 份 \n",
                    goods.getId(), goods.getGoodsName(), goods.getStockCount());
        }

        System.out.println("==============================================================");
        System.out.println("============== ✅ 缓存预热完成！高并发防线已就绪！=============");
        System.out.println("==============================================================");
    }
}