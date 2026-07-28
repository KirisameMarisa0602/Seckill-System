package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.entity.User;
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

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private DefaultRedisScript<Long> seckillScript;

    @RequestMapping(value = "/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(User user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        Long result = (Long) redisTemplate.execute(
                seckillScript,
                Arrays.asList(
                        "seckillGoods:" + goodsId,
                        "seckillUserOrder:" + user.getId() + ":" + goodsId
                )
        );
        if (result == null || result == 0L) {
            redisTemplate.opsForValue().set("isStockEmpty:" + goodsId, "0");
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        } else if (result == 2L) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        SeckillMessage message = new SeckillMessage(user, goodsId);
        mqSender.sendSeckillMessage(message);
        return RespBean.success(0);
    }

    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        Object orderIdStr = redisTemplate.opsForValue().get("seckillOrderCache:" + user.getId() + ":" + goodsId);
        if (orderIdStr != null) {
            return RespBean.success(Long.parseLong(orderIdStr.toString()));
        }
        boolean isStockEmpty = redisTemplate.hasKey("isStockEmpty:" + goodsId);
        if (isStockEmpty) {
            return RespBean.success(-1);
        }
        return RespBean.success(0);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.findGoodsVo();
        if (goodsList == null) {
            return;
        }
        System.out.println("==============================================================");
        System.out.println("======== 【缓存预热 Cache Warm-up】 ========");
        System.out.println("==============================================================");
        for (GoodsVo goods : goodsList) {
            redisTemplate.opsForValue().set("seckillGoods:" + goods.getId(), goods.getStockCount());
            if (goods.getStockCount() > 0) {
                redisTemplate.delete("isStockEmpty:" + goods.getId());
            } else {
                redisTemplate.opsForValue().set("isStockEmpty:" + goods.getId(), "0");
            }
            System.out.printf(" 加载商品 | ID: %-2d | 名称: %-15s | 注入 Redis 秒杀库存数: %d 份 \n",
                    goods.getId(), goods.getGoodsName(), goods.getStockCount());
        }
        System.out.println("==============================================================");
        System.out.println("============== 缓存预热完成！=============");
        System.out.println("==============================================================");
    }
}