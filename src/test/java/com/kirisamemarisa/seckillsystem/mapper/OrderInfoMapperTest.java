package com.kirisamemarisa.seckillsystem.mapper;

import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@SpringBootTest
@Transactional
public class OrderInfoMapperTest {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Test
    public void testOrderInfoInsert() {
        OrderInfo orderInfo = OrderInfo.builder()
                .userId(13812345678L)
                .goodsId(1L)
                .deliveryAddrId(0L)
                .goodsName("iPhone 15")
                .goodsCount(1)
                .goodsPrice(new BigDecimal("5999.00")) // 秒杀抢到的价格可能不同于原价
                .orderChannel(1)
                .status(0) // 0表示未支付
                .createDate(new Date())
                .build();

        int result = orderInfoMapper.insert(orderInfo);
        Assertions.assertEquals(1, result);
        Assertions.assertNotNull(orderInfo.getId());

        OrderInfo query = orderInfoMapper.selectById(orderInfo.getId());
        Assertions.assertEquals(0, query.getStatus(), "订单初始状态应为未支付");
    }
}