package com.kirisamemarisa.seckillsystem.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GoodsMapperTest {
    @Test
    void mainStockDecrementIsGuardedAgainstNegativeInventory() throws Exception {
        Update update = GoodsMapper.class
                .getMethod("decrementGoodsStock", Long.class)
                .getAnnotation(Update.class);
        assertTrue(String.join(" ", update.value()).contains("goods_stock > 0"));
    }
}
