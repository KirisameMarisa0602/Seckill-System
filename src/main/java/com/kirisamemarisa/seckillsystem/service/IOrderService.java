package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import java.math.BigDecimal;

public interface IOrderService extends IService<OrderInfo> {
    // 创建秒杀订单
    OrderInfo createSeckillOrder(Long userId, GoodsVo goods);
    Long findSeckillOrderId(Long userId, Long goodsId);
    // 取消超时订单
    void cancelTimeoutOrder(Long orderId);
    PaymentResult paySuccess(Long orderId, String tradeNo, BigDecimal amount,
                             String appId, String sellerId);
}