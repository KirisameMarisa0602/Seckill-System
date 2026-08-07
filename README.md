# 企业级高并发秒杀系统 (Seckill-System V7.0)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-9.x-blue.svg)
![Redis](https://img.shields.io/badge/Redis-高性能混合缓存-red.svg)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-异步削峰&死信自愈-orange.svg)

## 项目简介

本项目是一个面向生产环境的、经历过万级并发洪峰压测的**企业级高可用秒杀系统**。
项目从最基础的裸奔直连架构引发超卖为起点，历经七次大规模底层重构，彻底解决了**高并发超卖、缓存穿透/击穿、恶意接口防刷、海量流量削峰、宕机容灾、幽灵售罄**等真实业务挑战，最终成功打通支付宝沙箱支付闭环。

## 核心技术栈

- **后端层**: Spring Boot 3.2.6, MyBatis-Plus
- **数据层**: MySQL 9.4.0, Redis, Redisson (布隆过滤器/分布式锁/延迟队列)
- **中间件**: RabbitMQ (Topic/死信/延迟队列机制)
- **安全化**: 双重 MD5, AOP 全局限流, 动态路由令牌
- **本地缓存**: Caffeine Local Cache (抵挡售罄穿透透传)

## 架构核心演进与亮点

- [x] **V1.0 - 内存原子防超卖**: 摒弃 DB 悲观锁，采用 `Redis Lua` 脚本实现极速且强一致性的库存预扣减，达成 0 超卖。
- [x] **V2.0 - MQ 异步流量削峰**: 引入 RabbitMQ，将秒杀请求转入异步排队，将万级 DB 单点写入并发骤降拉平，保护数据库免于崩溃。
- [x] **V3.0 - 零信任分布式鉴权**: 废除 Session，基于 Redis 打造去中心化 Token 体系；AOP 底层注入，实现一人一单严格物理拦截。
- [x] **V4.0 - 柔性动态安全防刷**: 引入动态算术验证码限制人工频次；秒杀真实接口通过获取时效性 UUID 动态下发，屏蔽黑客脚本直刷。
- [x] **V5.0 - 限流与异常降级**: 依托 Redis + Lua 固定窗口做 AOP 层限流 (`@AccessLimit`)；全局异常统管消除 500 报错。
- [x] **V6.0 - 终结幽灵售罄**: 利用 **Redis Pub/Sub 广播机制**，打破分布式单点本地缓存限制。在死信队列（订单超时未支付）或管理端回补库存时，瞬间通知全集群 JVM 清理售罄标记，库存涅槃重生！
- [x] **V7.0 - 支付全闭环压测**: Nginx 负载均衡下接入 JMeter 万级并发压测，0 超卖、0 死锁，顺利拉起支付宝沙箱收银台完成金流闭环。

## 快速启动

1. 在 MySQL 执行 `sql/schema.sql` 建表。
2. 启动 Redis 与 RabbitMQ。
3. 修改 `application.yml` 数据源与沙箱密钥。
4. 启动后端，调用 `/admin/warmup` 完成全量数据与布隆过滤器预热。