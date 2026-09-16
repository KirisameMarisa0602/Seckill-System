package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.manager.LocalCacheManager;
import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
import com.kirisamemarisa.seckillsystem.redis.OrderKey;
import com.kirisamemarisa.seckillsystem.redis.SeckillKey;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.api.RRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import com.kirisamemarisa.seckillsystem.exception.GlobalException;

@Slf4j
@RestController
@RequestMapping("/seckill")
public class SeckillController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private DefaultRedisScript<Long> seckillScript;

    @Autowired private RedissonClient redissonClient;

    @Autowired private IGoodsService goodsService;

    @Autowired private IOrderService orderService;

    @Autowired private LocalCacheManager cacheManager;

    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    @AccessLimit(second = 5, maxCount = 10, needLogin = true)
    public RespBean doSeckill(@PathVariable("path") String path, User user, Long goodsId) {
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
        if (goodsId == null || goodsId <= 0 || !StringUtils.hasText(path)) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goodsVo == null || goodsVo.getStartDate() == null || goodsVo.getEndDate() == null
                || goodsVo.getSeckillPrice() == null) {
            return RespBean.error(RespBeanEnum.SECKILL_NOT_START);
        }
        long now = System.currentTimeMillis();
        long startTime = goodsVo.getStartDate().atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        long endTime = goodsVo.getEndDate().atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        if(now < startTime || now > endTime){
            return RespBean.error(RespBeanEnum.SECKILL_NOT_START);
        }
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("seckill:rateLimiter:" + goodsId);
        if (!rateLimiter.tryAcquire(1)) {
            log.warn("【令牌桶限流触发】请求已被抛弃：流量过载！商品ID：{}，拦截的用户ID：{}", goodsId, user.getId());
            return RespBean.error(RespBeanEnum.RATE_LIMIT_ERROR);
        }
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckillGoodsBloomFilter");
        if (!bloomFilter.contains(goodsId)) {
            log.warn("检测到恶意穿透请求，非法的商品ID: {}", goodsId);
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        Boolean over = cacheManager.checkEmpty(goodsId);
        if (over != null && over) { return RespBean.error(RespBeanEnum.EMPTY_STOCK); }
        String pathKey = SeckillKey.getSeckillPath.getPrefix() + user.getId() + ":" + goodsId;
        String realPath = (String) redisTemplate.opsForValue().get(pathKey);
        if (!path.equals(realPath)) { return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL); }
        long expireSeconds = (endTime - now) / 1000;
        if (expireSeconds <= 0) expireSeconds = 3600;
        String eventId = UUID.randomUUID().toString().replace("-", "");
        Long result = stringRedisTemplate.execute(
                seckillScript,
                Arrays.asList(
                        GoodsKey.getSeckillGoodsStock.getPrefix() + goodsId,
                        OrderKey.seckillUserOrder.getPrefix() + user.getId() + ":" + goodsId,
                        GoodsKey.isStockEmpty.getPrefix() + goodsId,
                        SeckillKey.outboxPending.getPrefix(),
                        SeckillKey.outboxEvent.getPrefix()
                ),
                String.valueOf(expireSeconds),
                eventId,
                String.valueOf(user.getId()),
                String.valueOf(goodsId),
                goodsVo.getGoodsName(),
                goodsVo.getSeckillPrice().toPlainString(),
                String.valueOf(System.currentTimeMillis())
        );
        if (result == null || result == 0L) {
            cacheManager.putEmpty(goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        } else if (result == 2L) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        return RespBean.success(0);
    }

    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId) {
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
        if (goodsId == null || goodsId <= 0) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        Object orderIdStr = redisTemplate.opsForValue().get(OrderKey.seckillOrderCache.getPrefix() + user.getId() + ":" + goodsId);
        if (orderIdStr != null) { return RespBean.success(String.valueOf(orderIdStr)); }
        Long persistedOrderId = orderService.findSeckillOrderId(user.getId(), goodsId);
        if (persistedOrderId != null) {
            redisTemplate.opsForValue().set(
                    OrderKey.seckillOrderCache.getPrefix() + user.getId() + ":" + goodsId,
                    persistedOrderId,
                    OrderKey.seckillOrderCache.expireSeconds(),
                    TimeUnit.SECONDS
            );
            return RespBean.success(String.valueOf(persistedOrderId));
        }
        boolean isStockEmpty = stringRedisTemplate.hasKey(GoodsKey.isStockEmpty.getPrefix() + goodsId);
        if (isStockEmpty) { return RespBean.success(-1); }
        return RespBean.success(0);
    }

    @AccessLimit(second = 5, maxCount = 5, needLogin = true)
    @GetMapping(value = "/captcha")
    public void getCaptcha(User user, @RequestParam("goodsId") Long goodsId, HttpServletResponse response) {
        if (user == null || goodsId == null || goodsId <= 0) {
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32);
        String captchaKey = SeckillKey.getSeckillCaptcha.getPrefix() + user.getId() + ":" + goodsId;
        redisTemplate.opsForValue().set(captchaKey, captcha.text(), SeckillKey.getSeckillCaptcha.expireSeconds(), TimeUnit.SECONDS);
        try { captcha.out(response.getOutputStream()); } catch (Exception e) { log.error("验证码生成失败", e); }
    }

    @AccessLimit(second = 5, maxCount = 5, needLogin = true)
    @GetMapping(value = "/path")
    @ResponseBody
    public RespBean getSeckillPath(User user, Long goodsId, String captcha) {
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
        if (goodsId == null || goodsId <= 0) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goods == null || goods.getStartDate() == null || goods.getEndDate() == null) {
            return RespBean.error(RespBeanEnum.BIND_ERROR);
        }
        long now = System.currentTimeMillis();
        long startTime = goods.getStartDate().atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        long endTime = goods.getEndDate().atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        if (now < startTime || now > endTime) { return RespBean.error(RespBeanEnum.SECKILL_NOT_START); }
        if (!StringUtils.hasText(captcha)) { return RespBean.error(RespBeanEnum.CAPTCHA_ERROR); }
        String captchaKey = SeckillKey.getSeckillCaptcha.getPrefix() + user.getId() + ":" + goodsId;
        String realCaptcha = (String) redisTemplate.opsForValue().get(captchaKey);
        if (!captcha.equals(realCaptcha)) { return RespBean.error(RespBeanEnum.CAPTCHA_ERROR); }
        redisTemplate.delete(captchaKey);
        String str = UUID.randomUUID().toString().replace("-", "");
        String pathKey = SeckillKey.getSeckillPath.getPrefix() + user.getId() + ":" + goodsId;
        redisTemplate.opsForValue().set(pathKey, str, SeckillKey.getSeckillPath.expireSeconds(), TimeUnit.SECONDS);
        return RespBean.success(str);
    }
}