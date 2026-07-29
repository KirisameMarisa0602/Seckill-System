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
import java.util.Arrays;

@RestController
@RequestMapping("/seckill")
public class SeckillController{
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
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
                        "seckillUserOrder:" + user.getId() + ":" + goodsId,
                        "isStockEmpty:" + goodsId
                )
        );
        if (result == null || result == 0L) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        } else if (result == 2L) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        SeckillMessage message = new SeckillMessage(user.getId(), goodsId);
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
}