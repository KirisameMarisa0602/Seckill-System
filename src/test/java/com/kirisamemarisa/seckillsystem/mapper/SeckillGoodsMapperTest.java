package com.kirisamemarisa.seckillsystem.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证秒杀库存扣减 SQL 契约：条件必须包含 {@code stock_count > 0}，防止超卖。
 */
class SeckillGoodsMapperTest {

    /**
     * 断言 {@code decrementStock} 的 UPDATE 带库存大于 0 的守卫条件。
     */
    @Test
    void seckillStockDecrementIsGuardedAgainstOverselling() throws Exception {
        Update update = SeckillGoodsMapper.class
                .getMethod("decrementStock", Long.class)
                .getAnnotation(Update.class);
        assertTrue(String.join(" ", update.value()).contains("stock_count > 0"));
    }
}
