package com.kirisamemarisa.seckillsystem.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证秒杀订单表幂等契约：同一用户对同一商品只能有一行订单。
 *
 * <p>MQ 重复投递依赖库表唯一索引拦截，不能仅靠应用层判断。
 */
class SeckillOrderMapperTest {

    /**
     * 断言 Flyway V1 含 {@code uk_seckill_order_user_goods (user_id, goods_id)}。
     */
    @Test
    void schemaEnforcesOneOrderPerUserAndGoods() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V1__init_schema.sql")) {
            assertNotNull(input);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("UNIQUE KEY uk_seckill_order_user_goods (user_id, goods_id)"));
        }
    }
}
