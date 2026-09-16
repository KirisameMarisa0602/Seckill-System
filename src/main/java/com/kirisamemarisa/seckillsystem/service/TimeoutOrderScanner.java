package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class TimeoutOrderScanner {
    @Autowired private IOrderService orderService;

    @Scheduled(fixedDelayString = "${seckill.order-timeout.scan-interval-ms:60000}")
    public void closeExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        List<OrderInfo> expiredOrders = orderService.list(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getStatus, 0)
                        .le(OrderInfo::getCreateDate, deadline)
                        .orderByAsc(OrderInfo::getCreateDate)
                        .last("LIMIT 100")
        );
        for (OrderInfo order : expiredOrders) {
            try {
                orderService.cancelTimeoutOrder(order.getId());
            } catch (Exception e) {
                log.error("数据库兜底关单失败，订单: {}", order.getId(), e);
            }
        }
    }
}
