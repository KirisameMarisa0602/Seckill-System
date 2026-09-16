package com.kirisamemarisa.seckillsystem.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeckillGoodsMapperTest {
    @Test
    void seckillStockDecrementIsGuardedAgainstOverselling() throws Exception {
        Update update = SeckillGoodsMapper.class
                .getMethod("decrementStock", Long.class)
                .getAnnotation(Update.class);
        assertTrue(String.join(" ", update.value()).contains("stock_count > 0"));
    }
}
