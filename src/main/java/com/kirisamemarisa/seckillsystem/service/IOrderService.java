package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import java.math.BigDecimal;

public interface IOrderService extends IService<OrderInfo> {

    OrderInfo createSeckillOrder(Long userId, GoodsVo goods, String eventId);

    Long findSeckillOrderId(Long userId, Long goodsId);

    void cancelTimeoutOrder(Long orderId);

    PaymentResult paySuccess(Long orderId, String tradeNo, BigDecimal amount,
                             String appId, String sellerId);
}
