package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付订单的数据库兜底扫描。与 RabbitMQ 死信关单互补：
 * 消息丢失时仍能把 {@code t_order.status=0} 且创建超过 15 分钟的单据关掉。
 * 真正改状态、回库存走 {@link IOrderService#cancelTimeoutOrder}。
 */
@Slf4j
@Component
public class TimeoutOrderScanner {
    @Autowired private IOrderService orderService;

    /**
     * 默认每 60 秒扫一批。{@code last("LIMIT 100")} 拼在 MP SQL 末尾，避免一次锁太多行。
     * 单笔失败只打日志，不中断本轮剩余订单。
     */
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
