# 高并发秒杀系统 (Seckill-System)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-9.x-blue.svg)
![Redis](https://img.shields.io/badge/Redis-高性能混合缓存-red.svg)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-异步削峰&死信自愈-orange.svg)

## 项目简介

本项目是一个用于演示高并发秒杀关键技术与一致性治理的 Spring Boot 单体应用。
项目从最基础的裸奔直连架构引发超卖为起点，历经七次大规模底层重构，彻底解决了**高并发超卖、缓存穿透/击穿、恶意接口防刷、海量流量削峰、宕机容灾、幽灵售罄**等真实业务挑战，最终成功打通支付宝沙箱支付闭环。

## 核心技术栈

- **后端层**: Spring Boot 3.5.16, MyBatis-Plus 3.5.17
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

1. 启动 MySQL、Redis 与 RabbitMQ。
2. 设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、Redis、RabbitMQ 和支付宝相关环境变量。
3. 启动后端；Flyway 会自动执行 `src/main/resources/db/migration` 中的版本化建表脚本。
4. 启动时会执行安全预热：只初始化不存在的 Redis 库存键，不会覆盖运行中的预扣库存。

也可以复制 `.env.example` 为本地 `.env`、替换全部示例密钥后执行 `docker compose up --build`。

必须提供的环境变量：

- 数据库：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- Redis：无密码时不要设置密码变量；启用认证时设置 `SPRING_DATA_REDIS_PASSWORD`
- RabbitMQ：`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`
- 支付宝：`ALIPAY_APP_ID`、`ALIPAY_SELLER_ID`、`ALIPAY_PRIVATE_KEY`、`ALIPAY_PUBLIC_KEY`、`ALIPAY_NOTIFY_URL`、`ALIPAY_RETURN_URL`
- 首次创建管理员时可临时设置：`BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_PASSWORD`

订单状态：`0` 待支付、`1` 已支付、`-1` 已取消、`-2` 已收款待退款/人工处理。

生产部署前必须执行 `mvn verify`，并验证支付回调、MQ 故障重试、超时关单和 Redis/MySQL 库存对账。