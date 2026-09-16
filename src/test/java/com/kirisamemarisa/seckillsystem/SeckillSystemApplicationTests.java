package com.kirisamemarisa.seckillsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证启动类契约：必须同时开启 Spring Boot 与定时任务。
 *
 * <p>Outbox 扫描、超时关单兜底依赖 {@code @EnableScheduling}，缺失会导致消息滞留或关单失效。
 */
class SeckillSystemApplicationTests {

    /**
     * 断言启动类同时标注 {@code @SpringBootApplication} 与 {@code @EnableScheduling}。
     */
    @Test
    void applicationEnablesBootAndScheduledRecoveryTasks() {
        assertNotNull(SeckillSystemApplication.class.getAnnotation(SpringBootApplication.class));
        assertNotNull(SeckillSystemApplication.class.getAnnotation(EnableScheduling.class));
    }
}
