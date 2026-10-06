package com.kirisamemarisa.seckillsystem.service;

import com.kirisamemarisa.seckillsystem.entity.DeliveryAddress;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.OrderStatus;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.exception.InsufficientStockException;
import com.kirisamemarisa.seckillsystem.mapper.SeckillGoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.service.DeliveryAddressService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private PaymentRecordMapper paymentRecordMapper;
    @Mock private GoodsMapper goodsMapper;
    @Mock private SeckillGoodsMapper seckillGoodsMapper;
    @Mock private SeckillOrderMapper seckillOrderMapper;
    @Mock private DeliveryAddressService deliveryAddressService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "baseMapper", orderInfoMapper);
    }

    @Test
    void pendingOrderBecomesPaidAtomically() {
        OrderInfo order = pendingOrder();
        when(orderInfoMapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(goodsMapper.decrementGoodsStock(10L)).thenReturn(1);

        PaymentResult result = orderService.paySuccess(
                1L, "trade-1", new BigDecimal("9.90"), "app", "seller");

        assertEquals(PaymentResult.PAID, result);
        assertEquals(OrderStatus.PAID.code(), order.getStatus());
        verify(paymentRecordMapper).insert(any(PaymentRecord.class));
    }

    @Test
    void canceledOrderCreatesRefundWorkItemWithoutConsumingStock() {
        OrderInfo order = pendingOrder();
        order.setStatus(OrderStatus.CANCELED.code());
        when(orderInfoMapper.selectByIdForUpdate(1L)).thenReturn(order);

        PaymentResult result = orderService.paySuccess(
                1L, "trade-2", new BigDecimal("9.90"), "app", "seller");

        assertEquals(PaymentResult.REFUND_PENDING, result);
        assertEquals(OrderStatus.REFUND_PENDING.code(), order.getStatus());
        verify(goodsMapper, never()).decrementGoodsStock(any());
        verify(paymentRecordMapper).insert(any(PaymentRecord.class));
    }

    private OrderInfo pendingOrder() {
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        order.setUserId(2L);
        order.setGoodsId(10L);
        order.setGoodsPrice(new BigDecimal("9.90"));
        order.setStatus(OrderStatus.UNPAID.code());
        return order;
    }

    @Test
    void createSeckillOrderCopiesDefaultAddressAndEventId() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<OrderInfo> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        when(seckillGoodsMapper.decrementStock(10L)).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        DeliveryAddress address = new DeliveryAddress();
        address.setId(7L);
        address.setReceiverName("测试用户甲");
        address.setReceiverPhone("13800138001");
        address.setDetail("杭州市西湖区龙井路 1 号");
        when(deliveryAddressService.findDefault(2L)).thenReturn(address);

        GoodsVo goods = new GoodsVo();
        goods.setId(10L);
        goods.setGoodsName("耳机");
        goods.setSeckillPrice(new BigDecimal("9.90"));
        OrderInfo created = orderService.createSeckillOrder(2L, goods, "event-1");

        assertEquals(OrderStatus.UNPAID.code(), created.getStatus());
        assertEquals(7L, created.getDeliveryAddrId());
        assertEquals("杭州市西湖区龙井路 1 号", created.getReceiverDetail());
        verify(seckillOrderMapper).insert(org.mockito.ArgumentMatchers.argThat((SeckillOrder row) ->
                "event-1".equals(row.getEventId()) && Long.valueOf(2L).equals(row.getUserId())));
    }

    @Test
    void createSeckillOrderThrowsWhenDatabaseStockIsGone() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<OrderInfo> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        when(seckillGoodsMapper.decrementStock(10L)).thenReturn(0);
        GoodsVo goods = new GoodsVo();
        goods.setId(10L);
        goods.setSeckillPrice(new BigDecimal("9.90"));

        assertThrows(InsufficientStockException.class,
                () -> orderService.createSeckillOrder(2L, goods, "event-2"));
    }
}
