package com.kirisamemarisa.seckillsystem.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证商品主库存扣减契约：UPDATE 必须带 {@code goods_stock > 0}，防止扣成负数。
 */
class GoodsMapperTest {

    /**
     * 断言 {@code decrementGoodsStock} 的 SQL 含库存大于 0 的守卫条件。
     */
    @Test
    void mainStockDecrementIsGuardedAgainstNegativeInventory() throws Exception {
        Update update = GoodsMapper.class
                .getMethod("decrementGoodsStock", Long.class)
                .getAnnotation(Update.class);
        assertTrue(String.join(" ", update.value()).contains("goods_stock > 0"));
    }
}
