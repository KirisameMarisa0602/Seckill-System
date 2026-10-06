package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.exception.InsufficientStockException;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MQReceiverTest {
    @Mock private IOrderService orderService;
    @Mock private SeckillOrderMapper seckillOrderMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ValueOperations<String, String> stringValueOperations;
    @Mock private DefaultRedisScript<Long> rollbackSeckillScript;
    @Mock private Channel channel;
    @InjectMocks private MQReceiver receiver;

    @Test
    void sameEventRedeliveryDoesNotRestoreStock() throws Exception {
        SeckillOrder existing = existingOrder("event-a");
        when(seckillOrderMapper.selectOne(any())).thenReturn(existing);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        receiver.receive(message("event-a"), channel, delivery(9L));

        verify(channel).basicAck(9L, false);
        verify(orderService, never()).createSeckillOrder(any(), any(), any());
        verify(stringRedisTemplate, never()).execute(any(), anyList());
    }

    @Test
    void differentEventRestoresTheExtraReservation() throws Exception {
        when(seckillOrderMapper.selectOne(any())).thenReturn(existingOrder("event-a"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        when(stringRedisTemplate.execute(any(), anyList())).thenReturn(1L);

        receiver.receive(message("event-b"), channel, delivery(9L));

        verify(channel).basicAck(9L, false);
        verify(stringRedisTemplate).execute(any(), anyList());
        verify(stringRedisTemplate).convertAndSend("stock_replenish_channel", "2");
        verify(stringValueOperations).set("OrderKey:seckillUserOrder:1:2", "1");
    }

    @Test
    void wrappedStockExceptionAlignsRedisWithoutRetry() throws Exception {
        when(seckillOrderMapper.selectOne(any())).thenReturn(null);
        when(orderService.createSeckillOrder(any(), any(), any()))
                .thenThrow(new IllegalStateException("tx", new InsufficientStockException()));
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);

        receiver.receive(message("event-c"), channel, delivery(4L));

        verify(channel).basicAck(4L, false);
        verify(channel, never()).basicNack(any(Long.class), any(Boolean.class), any(Boolean.class));
        verify(stringValueOperations).set("GoodsKey:stock:2", "0");
    }

    @Test
    void duplicateKeyOfAnotherEventRestoresStock() throws Exception {
        when(seckillOrderMapper.selectOne(any())).thenReturn(null, existingOrder("event-a"));
        when(orderService.createSeckillOrder(any(), any(), any()))
                .thenThrow(new DuplicateKeyException("uk_seckill_order_user_goods"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        when(stringRedisTemplate.execute(any(), anyList())).thenReturn(1L);

        receiver.receive(message("event-b"), channel, delivery(5L));

        verify(channel).basicAck(5L, false);
        verify(stringRedisTemplate).execute(any(), anyList());
    }

    private SeckillOrder existingOrder(String eventId) {
        SeckillOrder existing = new SeckillOrder();
        existing.setUserId(1L);
        existing.setGoodsId(2L);
        existing.setOrderId(3L);
        existing.setEventId(eventId);
        return existing;
    }

    private SeckillMessage message(String eventId) {
        return new SeckillMessage(1L, 2L, "goods", new BigDecimal("1.00"), eventId);
    }

    private Message delivery(long tag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(tag);
        return new Message(new byte[0], properties);
    }
}
