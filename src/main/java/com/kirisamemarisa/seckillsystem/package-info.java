/**
 * 秒杀系统后端根包。
 *
 * <p>这是一个 Spring Boot 单体应用：HTTP 入口在 {@code controller}，业务在 {@code service}，
 * 持久化在 {@code mapper}/{@code entity}，秒杀热点路径额外依赖 Redis Lua、本地 Caffeine 缓存和 RabbitMQ。
 *
 * <p>分层约定（保持现有目录，不再按功能竖切模块）：
 * <ul>
 *   <li>controller：只做参数校验、鉴权上下文与 RespBean 包装</li>
 *   <li>service：事务、库存、订单状态机、支付对账</li>
 *   <li>redis / rabbitmq / manager：基础设施适配，不直接对外暴露 HTTP</li>
 *   <li>vo：前后端 JSON 契约；entity：表结构映射</li>
 * </ul>
 */
package com.kirisamemarisa.seckillsystem;
