package com.kirisamemarisa.seckillsystem.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaContractTest {
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
