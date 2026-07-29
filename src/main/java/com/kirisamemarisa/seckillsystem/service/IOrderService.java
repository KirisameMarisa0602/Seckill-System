package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;

public interface IOrderService extends IService<OrderInfo> {
    OrderInfo createSeckillOrder(Long userId, GoodsVo goods);
}