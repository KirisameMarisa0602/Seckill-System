package com.kirisamemarisa.seckillsystem.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.rabbitmq.MQSender;
import com.kirisamemarisa.seckillsystem.redis.SeckillKey;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.api.RRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
public class SeckillController {
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private MQSender mqSender;

    @Autowired private DefaultRedisScript<Long> seckillScript;

    @Autowired private RedissonClient redissonClient;

    @Autowired private IGoodsService goodsService;

    private final Cache<Long, Boolean> emptyStockCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(@PathVariable("path") String path, User user, Long goodsId) {
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
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
        Boolean over = emptyStockCache.getIfPresent(goodsId);
        if (over != null && over) { return RespBean.error(RespBeanEnum.EMPTY_STOCK); }
        String pathKey = SeckillKey.getSeckillPath.getPrefix() + user.getId() + ":" + goodsId;
        String realPath = (String) redisTemplate.opsForValue().get(pathKey);
        if (!path.equals(realPath)) { return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL); }
        Long result = stringRedisTemplate.execute(
                seckillScript,
                Arrays.asList("seckillGoods:" + goodsId, "seckillUserOrder:" + user.getId() + ":" + goodsId, "isStockEmpty:" + goodsId)
        );
        if (result == null || result == 0L) {
            emptyStockCache.put(goodsId, true);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        } else if (result == 2L) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        mqSender.sendSeckillMessage(new SeckillMessage(user.getId(), goodsId));
        return RespBean.success(0);
    }

    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId) {
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
        Object orderIdStr = redisTemplate.opsForValue().get("seckillOrderCache:" + user.getId() + ":" + goodsId);
        if (orderIdStr != null) { return RespBean.success(Long.parseLong(orderIdStr.toString())); }
        boolean isStockEmpty = stringRedisTemplate.hasKey("isStockEmpty:" + goodsId);
        if (isStockEmpty) { return RespBean.success(-1); }
        return RespBean.success(0);
    }

    @AccessLimit(second = 5, maxCount = 5, needLogin = true)
    @GetMapping(value = "/captcha")
    public void getCaptcha(User user, @RequestParam("goodsId") Long goodsId, HttpServletResponse response) {
        if (user == null || goodsId < 0) { throw new RuntimeException("请求非法"); }
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32);
        String text = captcha.text();
        String captchaKey = SeckillKey.getSeckillCaptcha.getPrefix() + user.getId() + ":" + goodsId;
        redisTemplate.opsForValue().set(captchaKey, text, SeckillKey.getSeckillCaptcha.expireSeconds(), TimeUnit.SECONDS);
        try { captcha.out(response.getOutputStream()); } catch (Exception e) { log.error("验证码生成失败", e); }
    }

    @AccessLimit(second = 5, maxCount = 5, needLogin = true)
    @GetMapping(value = "/path")
    @ResponseBody
    public RespBean getSeckillPath(User user, Long goodsId, String captcha) {
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goods == null) { return RespBean.error(RespBeanEnum.BIND_ERROR); }
        long now = System.currentTimeMillis();
        if (now < goods.getStartDate().getTime() || now > goods.getEndDate().getTime()) { return RespBean.error(RespBeanEnum.SECKILL_NOT_START); }
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

    public void clearEmptyStock(Long goodsId) {
        emptyStockCache.invalidate(goodsId);
        log.info("【本地缓存防线同步】成功清空商品 {} 的本地售价空标记！", goodsId);
    }
}