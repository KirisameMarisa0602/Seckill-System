package com.kirisamemarisa.seckillsystem.mapper;

import com.kirisamemarisa.seckillsystem.entity.Goods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@Transactional
public class GoodsMapperTest {

    @Autowired
    private GoodsMapper goodsMapper;

    @Test
    public void testGoodsCrud() {
        // 创建基础商品
        Goods goods = Goods.builder()
                .goodsName("iPhone 15")
                .goodsTitle("Apple iPhone 15 256GB")
                .goodsImg("/img/iphone15.png")
                .goodsDetail("Apple新款手机，A16芯片...")
                .goodsPrice(new BigDecimal("6999.00"))
                .goodsStock(1000)
                .build();

        // 测试插入
        int result = goodsMapper.insert(goods);
        Assertions.assertEquals(1, result);
        Assertions.assertNotNull(goods.getId(), "自增ID应回填");

        // 测试查询
        Goods queriedGoods = goodsMapper.selectById(goods.getId());
        Assertions.assertEquals("iPhone 15", queriedGoods.getGoodsName());
    }
}