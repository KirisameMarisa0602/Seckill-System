package com.kirisamemarisa.seckillsystem.service;

import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.PaymentRecord;
import com.kirisamemarisa.seckillsystem.mapper.GoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.OrderInfoMapper;
import com.kirisamemarisa.seckillsystem.mapper.PaymentRecordMapper;
import com.kirisamemarisa.seckillsystem.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证支付回调契约：待支付订单原子转已支付；已关单则记待退款且不扣主库存。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private PaymentRecordMapper paymentRecordMapper;
    @Mock private GoodsMapper goodsMapper;
    @InjectMocks private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "baseMapper", orderInfoMapper);
    }

    /**
     * 待支付订单 {@code paySuccess}：状态变为已支付，并写入支付流水。
     */
    @Test
    void pendingOrderBecomesPaidAtomically() {
        OrderInfo order = pendingOrder();
        when(orderInfoMapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(goodsMapper.decrementGoodsStock(10L)).thenReturn(1);

        PaymentResult result = orderService.paySuccess(
                1L, "trade-1", new BigDecimal("9.90"), "app", "seller");

        assertEquals(PaymentResult.PAID, result);
        assertEquals(1, order.getStatus());
        verify(paymentRecordMapper).insert(any(PaymentRecord.class));
    }

    /**
     * 已取消订单迟到支付：标记待退款、不扣主库存，仍落支付流水。
     */
    @Test
    void canceledOrderCreatesRefundWorkItemWithoutConsumingStock() {
        OrderInfo order = pendingOrder();
        order.setStatus(-1);
        when(orderInfoMapper.selectByIdForUpdate(1L)).thenReturn(order);

        PaymentResult result = orderService.paySuccess(
                1L, "trade-2", new BigDecimal("9.90"), "app", "seller");

        assertEquals(PaymentResult.REFUND_PENDING, result);
        assertEquals(-2, order.getStatus());
        verify(goodsMapper, never()).decrementGoodsStock(any());
        verify(paymentRecordMapper).insert(any(PaymentRecord.class));
    }

    private OrderInfo pendingOrder() {
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        order.setUserId(2L);
        order.setGoodsId(10L);
        order.setGoodsPrice(new BigDecimal("9.90"));
        order.setStatus(0);
        return order;
    }
}
