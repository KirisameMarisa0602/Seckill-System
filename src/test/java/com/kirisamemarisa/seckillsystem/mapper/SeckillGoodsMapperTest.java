package com.kirisamemarisa.seckillsystem.mapper;

import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@SpringBootTest
@Transactional
public class SeckillGoodsMapperTest {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Test
    public void testSeckillGoodsCrud() {
        // 1. 构建秒杀商品数据
        SeckillGoods seckillGoods = SeckillGoods.builder()
                .goodsId(1001L) // 假设关联的商品ID
                .seckillPrice(new BigDecimal("9.99")) // 测试 BigDecimal
                .stockCount(100) // 秒杀库存
                .startDate(new Date())
                .endDate(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 一小时后结束
                .build();

        // 2. 测试插入 (主键自增)
        seckillGoodsMapper.insert(seckillGoods);
        Assertions.assertNotNull(seckillGoods.getId(), "插入后 MyBatis-Plus 会回写自增 ID");

        // 3. 测试更新库存 (模拟秒杀扣库存基础动作)
        seckillGoods.setStockCount(99);
        int updateResult = seckillGoodsMapper.updateById(seckillGoods);
        Assertions.assertEquals(1, updateResult, "应该成功更新一条记录");

        // 4. 测试查询
        SeckillGoods queried = seckillGoodsMapper.selectById(seckillGoods.getId());
        Assertions.assertEquals(99, queried.getStockCount(), "库存应该更新为 99");
    }
}