package com.kirisamemarisa.seckillsystem.mapper;

import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class SeckillOrderMapperTest {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Test
    public void testInsertAndUniqueIndex() {
        Long testUserId = 13800001111L;
        Long testGoodsId = 1001L;

        // 1. 模拟用户第一次秒杀成功，生成秒杀订单
        SeckillOrder order1 = SeckillOrder.builder()
                .userId(testUserId)
                .goodsId(testGoodsId)
                .orderId(8888L) // 关联的普通订单ID
                .build();
        int insertResult1 = seckillOrderMapper.insert(order1);
        Assertions.assertEquals(1, insertResult1, "第一次插入应该成功");

        // 2. 模拟黑客或并发情况下，同一个用户针对同一个商品再次生成订单
        SeckillOrder order2 = SeckillOrder.builder()
                .userId(testUserId)
                .goodsId(testGoodsId)
                .orderId(9999L)
                .build();

        // 3. 断言：期待抛出违反数据完整性的异常（因为触发了联合唯一索引限制）
        // 这样可以证明我们的数据库底座对“防刷/防超卖”是有托底保障的
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            seckillOrderMapper.insert(order2);
        }, "由于防复购唯一索引存在，第二次重复插入相同 userId 和 goodsId 必须报错");
    }
}