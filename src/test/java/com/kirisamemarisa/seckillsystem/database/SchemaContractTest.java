package com.kirisamemarisa.seckillsystem.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 Flyway V1 关键约束与索引契约。
 *
 * <p>覆盖秒杀订单唯一键、支付流水交易号唯一键、以及超时关单扫描所需的状态+创建时间索引。
 */
class SchemaContractTest {

    /**
     * 断言迁移脚本包含 {@code uk_seckill_order_user_goods}、{@code uk_payment_trade_no}、
     * {@code idx_order_status_create}。
     */
    @Test
    void migrationContainsCriticalUniquenessAndLookupIndexes() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V1__init_schema.sql")) {
            assertNotNull(input, "Flyway migration must be packaged");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("uk_seckill_order_user_goods"));
            assertTrue(sql.contains("uk_payment_trade_no"));
            assertTrue(sql.contains("idx_order_status_create"));
        }
    }
}
