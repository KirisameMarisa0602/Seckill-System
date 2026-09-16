package com.kirisamemarisa.seckillsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.PaymentRecord;
import com.kirisamemarisa.seckillsystem.mapper.PaymentRecordMapper;
import com.kirisamemarisa.seckillsystem.config.CacheWarmUpRunner;
import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import com.kirisamemarisa.seckillsystem.service.IAdminService;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.vo.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 运营后台 REST 入口，对应前端 {@code adminApi}（AdminLoginView / AdminView）。
 *
 * <p>处于秒杀链路的运营侧：账号登录、用户/商品维护、缓存预热，以及支付异常后的待退款工单查询。
 * 除 {@code /admin/login} 外需携带 {@code Admin-Token}。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired private IAdminService adminService;

    @Autowired private IGoodsService goodsService;

    @Autowired private IUserService userService;

    @Autowired private CacheWarmUpRunner cacheWarmUpRunner; // 与启动预热共用同一套 Runner，避免再写一份预热逻辑

    @Autowired private PaymentRecordMapper paymentRecordMapper;

    /**
     * 管理员登录，对应 {@code POST /admin/login}、{@code adminApi.login}。
     *
     * @param vo 用户名与密码；{@code @Valid} 先校验再进方法
     * @return 成功时 {@code obj} 为管理员 token；失败为登录错误码
     * @implNote 读管理员表；可能把历史 MD5 密码升级为 BCrypt 写回 DB；token 写入 Redis
     */
    @AccessLimit(second = 60, maxCount = 5, needLogin = false)
    @PostMapping("/login")
    @ResponseBody
    public RespBean login(@Valid @RequestBody AdminLoginVo vo) {
        return adminService.login(vo);
    }

    /**
     * 分页查询用户摘要，对应 {@code GET /admin/user/list}、{@code adminApi.users}。
     *
     * @param page     页码，从 1 起；与 {@code pageSize} 都为空时返回全量列表
     * @param pageSize 每页条数，缺省 20，上限 100
     * @return {@code obj} 为 {@link UserSummaryVo} 列表或 {@link PageResult}
     * @implNote 只读用户表，不写 Redis/MQ
     */
    @GetMapping("/user/list")
    @ResponseBody
    public RespBean getUserList(@RequestParam(required = false) Integer page,
                                @RequestParam(required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            return RespBean.success(userService.list().stream().map(UserSummaryVo::from).toList());
        }
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        var result = userService.page(new Page<>(safePage, safePageSize));
        return RespBean.success(new PageResult<>(
                result.getTotal(), safePage, safePageSize,
                result.getRecords().stream().map(UserSummaryVo::from).toList()));
    }

    /**
     * 查询秒杀商品列表，对应 {@code GET /admin/goods/list}、{@code adminApi.goods}。
     *
     * @param page     页码；与 {@code pageSize} 都为空时返回全量 {@code GoodsVo}
     * @param pageSize 每页条数，缺省 20，上限 100
     * @return {@code obj} 为商品 VO 列表或分页结果
     * @implNote 只读商品/秒杀商品表
     */
    @GetMapping("/goods/list")
    @ResponseBody
    public RespBean getGoodsList(@RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            return RespBean.success(goodsService.findGoodsVo());
        }
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        return RespBean.success(new PageResult<>(
                goodsService.countSeckillGoods(), safePage, safePageSize,
                goodsService.findGoodsVoByLimit((safePage - 1) * safePageSize, safePageSize)));
    }

    /**
     * 上架秒杀商品，对应 {@code POST /admin/goods/add}、{@code adminApi.addGoods}。
     *
     * @param addGoodsVo 商品与秒杀场次信息
     * @return 成功文案中带新商品 ID
     * @implNote 事务写入商品表与秒杀商品表；提交后写 Redis 库存、布隆过滤器与令牌桶
     */
    @PostMapping("/goods/add")
    @ResponseBody
    public RespBean addSeckillGoods(@Valid @RequestBody AddGoodsVo addGoodsVo) {
        return goodsService.addSeckillGoods(addGoodsVo);
    }

    /**
     * 下架秒杀商品，对应 {@code POST /admin/goods/delete/{goodsId}}、{@code adminApi.deleteGoods}。
     *
     * @param goodsId 路径变量，商品主键
     * @return 成功或“仍有待支付订单”等业务错误
     * @implNote 删 DB 记录；提交后删 Redis 库存/空库存标记/商品缓存，并删除该商品令牌桶
     */
    @PostMapping("/goods/delete/{goodsId}")
    @ResponseBody
    public RespBean deleteGoods(@PathVariable Long goodsId) { // {goodsId} 从 URL 路径绑定
        return goodsService.deleteSeckillGoods(goodsId);
    }

    /**
     * 更新秒杀商品，对应 {@code POST /admin/goods/update}、{@code adminApi.updateGoods}。
     *
     * @param updateGoodsVo 待更新字段，未传的保持原值
     * @return 成功或库存/活动期限制类错误
     * @implNote 写商品与秒杀商品表；库存变更时同步 Redis
     */
    @PostMapping("/goods/update")
    @ResponseBody
    public RespBean updateGoods(@Valid @RequestBody UpdateGoodsVo updateGoodsVo) {
        return goodsService.updateSeckillGoods(updateGoodsVo);
    }

    /**
     * 手动触发缓存预热，对应 {@code POST /admin/warmup}、{@code adminApi.warmup}。
     *
     * @return 成功提示，或系统错误码
     * @implNote 预热布隆过滤器、商品 VO、Redis 可售库存（仅 setIfAbsent）、空库存标记与令牌桶；可能发布库存补货频道
     */
    @PostMapping("/warmup")
    @ResponseBody
    public RespBean cacheWarmUp() {
        try {
            cacheWarmUpRunner.run(null); // 非启动场景传入 null 参数，复用 ApplicationRunner 预热流程
            return RespBean.success("灾备重置与预热：环境完美恢复！");
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR);
        }
    }

    /**
     * 分页查询待退款支付记录，对应 {@code GET /admin/payment/refund-pending}、{@code adminApi.refunds}。
     *
     * @param page     页码，默认 1
     * @param pageSize 每页条数，默认 20，上限 100
     * @return {@code obj} 为 {@link PaymentRecord} 分页结果
     * @implNote 只读支付流水表中 {@code status=REFUND_PENDING} 的记录
     */
    @GetMapping("/payment/refund-pending")
    public RespBean getRefundPending(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<PaymentRecord> result = paymentRecordMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new QueryWrapper<PaymentRecord>()
                        .eq("status", "REFUND_PENDING")
                        .orderByAsc("create_date")
        );
        return RespBean.success(new PageResult<>(
                result.getTotal(), safePage, safePageSize, result.getRecords()));
    }
}
