package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;

public interface IOrderService extends IService<OrderInfo> {
    // 创建秒杀订单
    OrderInfo createSeckillOrder(Long userId, GoodsVo goods);
    // 取消超时订单
    void cancelTimeoutOrder(Long orderId);
    //供第三方支付沙箱回调的 “完全幂等” 入口
    boolean paySuccess(Long orderId);
}