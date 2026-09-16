package com.kirisamemarisa.seckillsystem.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeckillOrderMapperTest {
    @Test
    void schemaEnforcesOneOrderPerUserAndGoods() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V1__init_schema.sql")) {
            assertNotNull(input);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("UNIQUE KEY uk_seckill_order_user_goods (user_id, goods_id)"));
        }
    }
}
