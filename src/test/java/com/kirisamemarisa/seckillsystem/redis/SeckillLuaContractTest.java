package com.kirisamemarisa.seckillsystem.redis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证秒杀 Lua 与 Outbox 同脚本契约：预扣库存与写入 Outbox 必须原子完成；回补必须先确认预占存在。
 */
class SeckillLuaContractTest {

    /**
     * 断言 {@code seckill-stock.lua} 在同一脚本内 {@code DECR} 库存并 {@code HSET}/{@code ZADD} Outbox。
     */
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

    /**
     * 断言 {@code rollback-seckill.lua} 仅在用户预占 Key 存在时才 {@code INCR} 回补库存。
     */
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
