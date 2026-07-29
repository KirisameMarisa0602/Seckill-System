package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.rabbitmq.MQSender;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import com.kirisamemarisa.seckillsystem.utils.MD5Util;
import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@RestController
@RequestMapping("/seckill")
public class SeckillController{
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private DefaultRedisScript<Long> seckillScript;

    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(@PathVariable("path") String path, User user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        String pathKey = "seckill:path:" + user.getId() + ":" + goodsId;
        String realPath = (String) redisTemplate.opsForValue().get(pathKey);
        if (!path.equals(realPath)) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
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

    @GetMapping(value = "/captcha")
    public void getCaptcha(User user, @RequestParam("goodsId") Long goodsId, HttpServletResponse response) {
        if (user == null || goodsId < 0) {
            throw new RuntimeException("请求非法");
        }
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32);
        String text = captcha.text();
        redisTemplate.opsForValue().set("seckill:captcha:" + user.getId() + ":" + goodsId, text, 60, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        } catch (Exception e) {
            log.error("验证码生成失败", e);
        }
    }

    @AccessLimit(second = 5, maxCount = 5, needLogin = true)
    @GetMapping(value = "/path")
    @ResponseBody
    public RespBean getSeckillPath(User user, Long goodsId, String captcha) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        if (!StringUtils.hasText(captcha)) {
            return RespBean.error(RespBeanEnum.CAPTCHA_ERROR);
        }
        String captchaKey = "seckill:captcha:" + user.getId() + ":" + goodsId;
        String realCaptcha = (String) redisTemplate.opsForValue().get(captchaKey);

        if (!captcha.equals(realCaptcha)) {
            return RespBean.error(RespBeanEnum.CAPTCHA_ERROR);
        }
        redisTemplate.delete(captchaKey);
        String str = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("seckill:path:" + user.getId() + ":" + goodsId, str, 60, TimeUnit.SECONDS);
        return RespBean.success(str);
    }
}