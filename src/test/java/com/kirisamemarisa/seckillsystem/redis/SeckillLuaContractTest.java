package com.kirisamemarisa.seckillsystem.redis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeckillLuaContractTest {
    @Test
    void reservationAndOutboxAreCreatedByTheSameLuaScript() throws IOException {
        try (var input = getClass().getResourceAsStream("/scripts/seckill-stock.lua")) {
            assertNotNull(input);
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(script.contains("redis.call('decr', stockKey)"));
            assertTrue(script.contains("redis.call('hset', outboxEventKey"));
            assertTrue(script.contains("redis.call('zadd', outboxPendingKey"));
        }
    }

    @Test
    void rollbackRequiresAnExistingReservation() throws IOException {
        try (var input = getClass().getResourceAsStream("/scripts/rollback-seckill.lua")) {
            assertNotNull(input);
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(script.contains("exists', userOrderKey"));
            assertTrue(script.contains("redis.call('incr', stockKey)"));
        }
    }
}
