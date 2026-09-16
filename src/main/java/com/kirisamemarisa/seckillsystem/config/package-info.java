/**
 * Spring 配置与启动期装配。
 *
 * <p>包含 Web MVC（CORS、拦截器、参数解析）、Redis/RabbitMQ/MyBatis-Plus Bean、
 * 安全响应头、密码编码器、缓存预热、管理员引导账号以及库存恢复订阅。
 *
 * <p>{@code ApplicationRunner} 按 {@code @Order} 执行：先校验表约束，再按需创建引导管理员，最后安全预热 Redis 库存。
 */
package com.kirisamemarisa.seckillsystem.config;
