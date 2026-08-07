package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goods")
public class GoodsController {
    @Autowired private IGoodsService goodsService;

    @GetMapping("/list")
    public RespBean getList() { return RespBean.success(goodsService.findGoodsVo()); }

    @GetMapping("/detail/{goodsId}")
    public RespBean getDetail(@PathVariable Long goodsId) {
        return RespBean.success(goodsService.findGoodsVoByGoodsId(goodsId));
    }
}