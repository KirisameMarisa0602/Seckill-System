package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MQReceiverTest {
    @Mock private IOrderService orderService;
    @Mock private SeckillOrderMapper seckillOrderMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private Channel channel;
    @InjectMocks private MQReceiver receiver;

    @Test
    void duplicateDeliveryUsesDatabaseOrderAndAcknowledges() throws Exception {
        SeckillOrder existing = new SeckillOrder();
        existing.setUserId(1L);
        existing.setGoodsId(2L);
        existing.setOrderId(3L);
        when(seckillOrderMapper.selectOne(any())).thenReturn(existing);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(9L);
        receiver.receive(
                new SeckillMessage(1L, 2L, "goods", new BigDecimal("1.00")),
                channel,
                new Message(new byte[0], properties)
        );

        verify(channel).basicAck(9L, false);
        verify(orderService, never()).createSeckillOrder(any(), any());
    }
}
