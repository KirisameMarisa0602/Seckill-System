package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.PageResult;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * C 端商品浏览接口，对应前端 {@code goodsApi}（HomeView 列表、GoodsDetailView 详情）。
 *
 * <p>处于秒杀链路最前端的只读阶段：用户先看货、再去 {@link SeckillController} 抢购。不写 Redis/MQ。
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {
    @Autowired private IGoodsService goodsService;

    /**
     * 秒杀商品列表，对应 {@code GET /goods/list}、{@code goodsApi.list}。
     *
     * @param page     页码，从 1 起；与 {@code pageSize} 都为空时返回全量
     * @param pageSize 每页条数，缺省 20，上限 100
     * @return {@code obj} 为 {@code GoodsVo} 列表或 {@link PageResult}
     * @implNote 读库（及服务层可能命中的商品缓存），本方法不直接写缓存
     */
    @GetMapping("/list")
    public RespBean getList(@RequestParam(required = false) Integer page,
                            @RequestParam(required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            return RespBean.success(goodsService.findGoodsVo());
        }
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        long total = goodsService.countSeckillGoods();
        return RespBean.success(new PageResult<>(
                total,
                safePage,
                safePageSize,
                goodsService.findGoodsVoByLimit((safePage - 1) * safePageSize, safePageSize)
        ));
    }

    /**
     * 单个秒杀商品详情，对应 {@code GET /goods/detail/{goodsId}}、{@code goodsApi.detail}。
     *
     * @param goodsId 路径变量，商品主键
     * @return {@code obj} 为该商品的 {@code GoodsVo}（含秒杀价与时间窗）
     * @implNote 只读；服务层可能回源 DB 并回填 Redis 商品缓存
     */
    @GetMapping("/detail/{goodsId}")
    public RespBean getDetail(@PathVariable Long goodsId) { // {goodsId} 从 URL 路径绑定
        return RespBean.success(goodsService.findGoodsVoByGoodsId(goodsId));
    }
}
