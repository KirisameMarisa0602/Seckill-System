package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.config.CacheWarmUpRunner;
import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import com.kirisamemarisa.seckillsystem.service.IAdminService;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.vo.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired private IAdminService adminService;

    @Autowired private IGoodsService goodsService;

    @Autowired private IUserService userService;

    @Autowired private CacheWarmUpRunner cacheWarmUpRunner; // 直接挂载执行器

    @AccessLimit(second = 60, maxCount = 5, needLogin = false)
    @PostMapping("/login")
    @ResponseBody
    public RespBean login(@Valid @RequestBody AdminLoginVo vo) {
        return adminService.login(vo);
    }

    @GetMapping("/user/list")
    @ResponseBody
    public RespBean getUserList() { return RespBean.success(userService.list()); }

    @GetMapping("/goods/list")
    @ResponseBody
    public RespBean getGoodsList() { return RespBean.success(goodsService.findGoodsVo()); }

    @PostMapping("/goods/add")
    @ResponseBody
    public RespBean addSeckillGoods(@Valid @RequestBody AddGoodsVo addGoodsVo) {
        return goodsService.addSeckillGoods(addGoodsVo);
    }

    @PostMapping("/goods/delete/{goodsId}")
    @ResponseBody
    public RespBean deleteGoods(@PathVariable Long goodsId) {
        return goodsService.deleteSeckillGoods(goodsId);
    }

    @PostMapping("/goods/update")
    @ResponseBody
    public RespBean updateGoods(@Valid @RequestBody UpdateGoodsVo updateGoodsVo) {
        return goodsService.updateSeckillGoods(updateGoodsVo);
    }

    @PostMapping("/warmup")
    @ResponseBody
    public RespBean cacheWarmUp() {
        try {
            cacheWarmUpRunner.run(null); // 直接唤醒自动挂载脚本，消灭冗余代码
            return RespBean.success("灾备重置与预热：环境完美恢复！");
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR);
        }
    }
}