package com.kirisamemarisa.seckillsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SeckillSystemApplicationTests {
    @Test
    void applicationEnablesBootAndScheduledRecoveryTasks() {
        assertNotNull(SeckillSystemApplication.class.getAnnotation(SpringBootApplication.class));
        assertNotNull(SeckillSystemApplication.class.getAnnotation(EnableScheduling.class));
    }
}
