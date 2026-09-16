package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.PageResult;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goods")
public class GoodsController {
    @Autowired private IGoodsService goodsService;

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

    @GetMapping("/detail/{goodsId}")
    public RespBean getDetail(@PathVariable Long goodsId) {
        return RespBean.success(goodsService.findGoodsVoByGoodsId(goodsId));
    }
}