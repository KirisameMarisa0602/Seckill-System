package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;

public interface IOrderService extends IService<OrderInfo> {
    OrderInfo createSeckillOrder(User user, GoodsVo goods);
}