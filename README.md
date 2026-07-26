#  企业级高并发秒杀系统 (Seckill-System)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.7-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-9.4.0-blue.svg)
![Redis](https://img.shields.io/badge/Redis-高性能缓存-red.svg)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-异步削峰-orange.svg)

##  项目简介

企业级高并发秒杀/抢票系统。基于 Spring Boot 4.0.7 + Redis + RabbitMQ
构建，旨在解决高并发场景下的超卖、缓存穿透、分布式限流以及流量削峰等核心高可用问题。项目秉承规范的 DDD/MVC 分层架构思想，为应对超频并发请求提供一站式落地方案。

##  核心技术栈

- **后端核心**: Spring Boot 4.0.7, MyBatis Framework (Java 21)
- **数据存储**: MySQL 9.4.0 ( InnoDB )
- **分布式缓存**: Redis + Redisson (布隆过滤器 + 分布式锁)
- **本地缓存**: Guava Cache (应对极热点数据/防止 Redis 流量风暴)
- **消息队列**: RabbitMQ (异步下单削峰、死信队列延迟处理)
- **压测与调优**: JMeter, JUnit, JVM 调优

##  核心架构亮点 (TODO)

- [ ] **接口防刷与限流**: 基于 Redis + 拦截器实现动态黑名单与 IP 级别令牌桶限流。
- [ ] **防御缓存穿透**: 启动预热加载全局商品 ID 进 **布隆过滤器 (Bloom Filter)**，前置拦截恶意构造的无效并发请求。
- [ ] **库存极速且安全扣减**: 舍弃低效的 DB 悲观锁表，采用 **Redis Lua 脚本** 实现极其严苛且原子性的库存预扣减，确保 **0 超卖**。
- [ ] **异步抗压与流量削峰**: 缓存放行后即可响应前端（排队中），通过 **RabbitMQ** 异步处理耗时的 DB 入库排队操作，保障数据库不被洪峰突垮。
- [ ] **兜底一致性补偿**: 运用 RabbitMQ 死信队列 (DLX) 机制，若用户抢单后 15 分钟未支付，自动触发补偿路由，安全回放库存。
- [ ] **可靠消息投递**: 开启 MQ 生产端 `Confirm` 回调记录及消费端手动 `ACK`，配合本地消息表，确保订单流转消息 100% 成功。

##  快速启动

1. 导入并在本地运行 MySQL，执行项目根目录 `sql/schema.sql` 完成建表。
2. 配置并启动 Redis server 与 RabbitMQ。
3. 修改 `application.yml` 中的数据源及中间件连接配置。
4. 运行 `SeckillSystemApplication.java` 启动服务。

##  压测数据对比 (演进记录)

-  [V0.5] 原始 DB 悲观锁直连版：TPS `待测试`
-  [V1.0] 引入 Redis + MQ 终极架构版：TPS `待测试` (确保在 0 超卖前提下实现量级飞跃)