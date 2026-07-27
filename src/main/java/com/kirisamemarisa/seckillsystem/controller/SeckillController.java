package com.kirisamemarisa.seckillsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.mapper.UserMapper;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * V0.5: 原始版直连DB秒杀
     * 压测准备参数：/seckill/doSeckillV05?userId=13800138000&goodsId=1001
     */
    @PostMapping("/doSeckillV05")
    public RespBean doSeckillV05(Long userId, Long goodsId) {
        // 简单模拟获取当前登录用户
        if (userId == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }
        User user = userMapper.selectById(userId);
        if (user == null) { return RespBean.error(RespBeanEnum.USER_NOT_EXIST); }

        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);

        // 1. 判断库存
        if (goods.getStockCount() < 1) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 2. 根据用户ID和商品ID去DB查询该用户是否重复抢购
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>()
                .eq("user_id", user.getId())
                .eq("goods_id", goodsId));
        if (seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }

        // 3. 进入核心裸奔下单业务代码
        try {
            orderService.seckillV05(user, goods);
            return RespBean.success("秒杀成功！");
        } catch (Exception e) {
            // 这里可能会触发你之前写的 user_id & goods_id 联合唯一索引报错 (这是好事)
            return RespBean.error(RespBeanEnum.ERROR);
        }
    }
}