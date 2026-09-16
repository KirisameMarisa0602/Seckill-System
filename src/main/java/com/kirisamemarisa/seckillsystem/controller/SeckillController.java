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

/**
 * 秒杀写路径入口，对应前端 {@code seckillApi}（GoodsDetailView：验证码 → 隐藏路径 → 下单 → 轮询结果）。
 *
 * <p>链路位置：验证码与路径防刷之后，用 Lua 在 Redis 预扣库存并写入 Outbox；定时任务再投递 RabbitMQ，
 * 消费者落订单库。本类不直接写订单表。{@code User} 由 token 解析注入。
 */
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

    /**
     * 执行秒杀，对应 {@code POST /seckill/{path}/doSeckill}、{@code seckillApi.submit}。
     *
     * @param path    路径变量，须与 Redis 中该用户+商品的一次性秒杀路径一致
     * @param user    当前登录用户
     * @param goodsId 秒杀商品 ID（query/form 参数）
     * @return 成功时 {@code obj=0} 表示已受理、请轮询 {@link #getResult}；失败为库存空/重复/限流等
     * @implNote Lua 预扣 Redis 库存、写用户已购标记与 Outbox；库存空时写本地空库存缓存。真正订单由 MQ 异步落库
     */
    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    @AccessLimit(second = 5, maxCount = 10, needLogin = true)
    public RespBean doSeckill(@PathVariable("path") String path, User user, Long goodsId) { // {path} 为一次性隐藏地址，不是商品 ID
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
        if (result == null || result == 0L) { // Lua 0：库存键不存在或库存已扣尽
            cacheManager.putEmpty(goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        } else if (result == 2L) { // Lua 2：该用户已抢过此商品
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        // Lua 1：预扣成功并写入 Outbox，此处 0 表示排队中而非订单号
        return RespBean.success(0);
    }

    /**
     * 轮询秒杀结果，对应 {@code GET /seckill/result}、{@code seckillApi.result}。
     *
     * @param user    当前登录用户
     * @param goodsId 商品 ID
     * @return {@code obj}：订单号字符串表示成功；{@code -1} 售罄；{@code 0} 仍在排队
     * @implNote 先读 Redis 订单缓存，未命中再查库并回填 Redis；空库存标记只读 Redis
     */
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

    /**
     * 生成算术验证码图，对应 {@code GET /seckill/captcha}、{@code seckillApi.captcha}。
     *
     * @param user     当前登录用户
     * @param goodsId  商品 ID，验证码按用户+商品隔离
     * @param response 直接写出 {@code image/gif}，无 JSON 包装
     * @implNote 正确答案写入 Redis，短时过期；非法参数抛 {@link GlobalException}
     */
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

    /**
     * 校验验证码并下发一次性秒杀路径，对应 {@code GET /seckill/path}、{@code seckillApi.path}。
     *
     * @param user    当前登录用户
     * @param goodsId 商品 ID
     * @param captcha 用户提交的验证码文本
     * @return 成功时 {@code obj} 为隐藏路径字符串，随后拼进 {@code /seckill/{path}/doSeckill}
     * @implNote 读 Redis 验证码后删除（一次性）；新路径写入 Redis
     */
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
