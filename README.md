# Seckill-System · 高并发秒杀系统

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883.svg)](https://vuejs.org/)
[![Redis](https://img.shields.io/badge/Redis-Lua%20预扣库存-red.svg)](#技术栈)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Outbox%20削峰-orange.svg)](#技术栈)
[![Release](https://img.shields.io/badge/release-v9.0-blue.svg)](https://github.com/KirisameMarisa0602/Seckill-System/releases/tag/v9.0)

面向高并发抢购场景的全栈演示项目：Spring Boot 单体承接秒杀写路径，Vue 3 提供 C 端会场与 B 端运营台，库存以 Redis Lua 为准，订单经 RabbitMQ 异步落库，支付走支付宝沙箱异步回调。

当前稳定版本为 **v9.0**（`main` 上的 final tag）。它不是一次写完的 Demo，而是从「裸奔超卖」开始、用 JMeter 压测驱动、按版本沉淀到 `main` 的演进结果。

---

## 目录

- [它解决什么问题](#它解决什么问题)
- [技术栈](#技术栈)
- [开发过程](#开发过程)
- [版本演进](#版本演进)
- [技术含量与架构优势](#技术含量与架构优势)
- [AI 工具链](#ai-工具链)
- [仓库结构](#仓库结构)
- [快速开始](#快速开始)
- [业务链路](#业务链路)
- [测试与文档](#测试与文档)

---

## 它解决什么问题

秒杀的难点不在「把商品列表画出来」，而在瞬时流量下把下面几件事同时做对：

| 问题 | 本仓库的做法 |
| --- | --- |
| 超卖 | Redis Lua 原子预扣；DB 层 `stock_count > 0` 条件更新兜底 |
| 数据库被打穿 | 热路径不写 MySQL；MQ 削峰后异步落单 |
| 一人多单 | Redis 预占标记 + `t_seckill_order(user_id, goods_id)` 唯一索引 |
| 脚本刷接口 | 算术验证码、一次性动态 path、`@AccessLimit`、Redisson 令牌桶 |
| 缓存击穿 / 穿透 | 布隆过滤器、互斥锁回源、空对象占位、Caffeine 本地售罄缓存 |
| 超时未支付占库存 | 延迟队列 15 分钟关单 + 定时扫描兜底，Lua 回补 Redis |
| 幽灵售罄 | Redis Pub/Sub 通知各节点清本地售罄标记 |
| 支付对账 | 支付宝验签、金额校验、`trade_no` 幂等；关单后到账记待退款 |

C 端可注册登录、看会场、过验证码抢购、轮询结果、打开收银台；B 端可维护商品、预热缓存、查看待退款工单。

---

## 技术栈

**后端**

- Java 21 / Spring Boot 3.5.16 / Validation / Actuator + Prometheus
- MyBatis-Plus 3.5 / MySQL 9 / Flyway 版本化建表
- Redis（Lettuce 连接池）+ 自管 Key 前缀；Lua：预扣库存、限流、回滚
- Redisson：布隆过滤器、分布式锁、令牌桶、延迟双删队列
- RabbitMQ：下单队列、错误死信重试、TTL 延迟关单、主库存补偿
- Redis Outbox（ZSET + HASH）保证「预扣成功必投递」
- Caffeine 进程内售罄缓存
- 支付宝开放平台 SDK（电脑网站支付 + 异步 notify）
- BCrypt（登录升级路径仍兼容历史 MD5）

**前端**

- Vue 3.5 + TypeScript + Vite 8
- Vue Router / Pinia / Axios / Element Plus（按需引入）
- 开发环境 `/api` 代理到 8080/8081，生产可由 Nginx 反代

**工程化**

- GitHub Actions：`mvn verify` + JaCoCo；PR 依赖漏洞审查
- Docker / Compose 一键拉起 MySQL、Redis、RabbitMQ、后端
- 双实例脚本 `scripts/run-local.ps1`（8080 / 8081）便于本地验证负载与本地缓存广播

---

## 开发过程

工作方式是 **压测发现问题 → 改架构 → 再压测留证 → tag 沉淀到 `main`**，而不是先堆功能再补性能。

1. **基线**：Spring Boot + MySQL 直连下单（v0.5）。JMeter 1000 并发、库存 10，订单打出 100 条，确认是丢失更新而不是「库存变成负数」。
2. **热路径外移**：库存裁决放到 Redis Lua（v1.0），MySQL 不再被万级 `UPDATE` 正面冲击。
3. **写放大后移**：Lua 只预扣，订单经 RabbitMQ 异步落库（v2.x），并用死信 / 延迟队列把失败与超时纳入同一套消费模型。
4. **把秒杀接口当成攻击面**：去 Session、验证码、隐藏 path、限流、全局异常（v3–v5）。
5. **按分布式故障想问题**：本地 Caffeine 售罄标记在补货后会「假售罄」，用 Pub/Sub 解封（v6）；支付、关单、待退款状态机补齐（v7）。
6. **把接口变成产品**：Vue 会场 / 订单 / 运营台与支付宝沙箱联调（v8），再做一致性加固、注释与契约收口（v9.0）。

分支习惯：`dev` 承接日常提交与 PR，里程碑以 `vX.Y` tag 打在 `main` 上。`docs/` 里每个大版本都有压测或联调报告和截图，对应 tag 可回溯。

---

## 版本演进

| 版本 | Tag | 核心变化 | 报告 |
| --- | --- | --- | --- |
| 裸奔 | `v0.5` | DB 直连抢购，压测暴露严重超卖 | [V0.5](docs/V0.5压测和裸奔版问题检测报告.md) |
| 原子预扣 | `v1.0` | Redis + Lua，库存预扣 0 超卖 | [V1.0](docs/V1.0压测和引入Redis+Lua脚本修复超卖问题检测报告.md) |
| 异步削峰 | `v2.0` | RabbitMQ 下单排队，保护 MySQL | [V2.0](docs/V2.0压测和引入rabbitMQ流量削峰测试报告.md) |
| 闭环与泄漏 | `v2.1` / `v2.2` | 订单结果缓存、防重 Key TTL、死信容灾 | [V2.2](docs/V2.2压测以及检验报告.md) |
| 鉴权拆分 | `v3.0` | Redis Token；用户 / 管理员通道分离 | [V3.0](docs/V3.0测试以及检验报告.md) |
| 防刷 | `v4.0` | 算术验证码 + 一次性秒杀 path | [V4.0](docs/V4.0测试以及检验报告.md) |
| 限流降级 | `v5.0` | `@AccessLimit` + Redis Lua 窗口；全局异常 | [V5.0](docs/V5.0测试以及检验报告.md) |
| 分布式自愈 | `v6.0` / `v6.1` | 注册与后台；Pub/Sub 解除幽灵售罄 | [V6.0](docs/V6.0测试以及检验报告.md) |
| 支付闭环 | `v7.0` / `v7.1` | 支付宝沙箱；Nginx 双实例；全链路压测 | [V7.0](docs/V7.0压测以及系统全链路检验报告.md) |
| 前后端产品化 | `v8.0` | Vue 状态机、运营台、支付页跳转 | [V8.0](docs/V8.0前后端联调测试报告.md) |
| 收官 | **`v9.0`** | 支付错误改 JSON 契约、分页 `ORDER BY`、中文注释与工程收口 | 本 README |

秒杀写路径（现行）：

```
验证码 → 一次性 path → Lua 预扣 + Outbox
        → Outbox 发布器投递 MQ → 落 t_order / t_seckill_order
        → 延迟关单 / 支付回调改状态 → 成功则扣 t_goods 主库存
```

---

## 技术含量与架构优势

相对「能跑的秒杀 Demo」，本仓库更强调 **裁决点唯一、失败可补偿、前端不信任**。

- **库存以 Lua 为准**：预扣、一人一单标记、Outbox 写入在同一脚本里完成，避免「扣了库存但消息没发出」。
- **Outbox 而不是裸 `convertAndSend`**：发布失败按 ZSET score 退避重试；多实例用短 TTL 锁防重复投递。
- **关单与支付互斥**：`SELECT … FOR UPDATE`；超时回补秒杀库存，支付成功才扣主库存；关单后到账进入 `REFUND_PENDING`，不把钱当成有效成交。
- **缓存只在事务提交后改**：商品上下架、改库存走 `afterCommit`，降低回滚后的脏缓存窗口；更新后再投递 500ms 延迟双删。
- **预热不覆盖运行中库存**：`SETNX` 初始化可售库存，滚动发布不会把预扣结果冲掉。
- **安全默认偏紧**：密码与密钥走环境变量；CORS 白名单可关；反代 IP 才采信 `X-Forwarded-For`；雪花订单号 JSON 输出为字符串，避免 JS 精度丢失。
- **可观测**：Actuator / Prometheus Gauge（Outbox 积压、待退款、Redis 与 MySQL 秒杀库存不一致）。

这些选择对应的是真实故障模式（超卖、重复下单、消息丢失、假售罄、重复支付通知），而不是单纯堆中间件名词。

---

## AI 工具链

后期迭代明确使用 **Cursor AI 工具链**（Agent、仓库内检索、终端与 Git 操作）做收口，而不是用模型从头生成整仓后不再过目。

| 阶段 | 人做的 | Cursor 协助的 |
| --- | --- | --- |
| v0.5–v7 | 压测设计、中间件取舍、支付与限流方案、报告截图 | 局部实现与排查（按当时工作流） |
| v8 | 产品交互、沙箱与内网穿透联调 | Vue 页面、API 封装、状态机与后端路径对齐 |
| v9.0 | 确认契约与发布（tag / 合并 `main`↔`dev`） | 前后端链路核对、支付失败改为可解析 JSON、分页稳定性、中文注释格式统一、提交说明 |

使用原则：

- **热路径不交给模型「自由发挥」**：Lua、唯一索引、关单与支付状态机仍以代码和测试为准。
- **改完要能验证**：`mvn verify`、前端 `vue-tsc`；接口字段与 `frontend/src/api/index.ts` 对齐。
- **Git 由人确认后再推**：提交与 tag 在本地审过再上 GitHub。

后续若继续扩展（会场助手、运营 Copilot），计划把现有 HTTP API 当作 Tool，而不是让模型直接改库存。讨论记录见开发过程中的架构取舍，尚未合入 v9.0 运行时。

---

## 仓库结构

```
Seckill-System/
├── src/main/java/.../seckillsystem/
│   ├── controller/     # HTTP 入口，与 frontend/src/api 对应
│   ├── service/        # 事务、订单状态机、支付入账
│   ├── rabbitmq/       # 发送、消费、Outbox 发布
│   ├── redis/          # Key 前缀
│   ├── config/         # 拦截器、预热、安全头、MQ/Redis 装配
│   └── mapper/         # 注解 SQL，无 XML
├── src/main/resources/scripts/   # seckill-stock / rate-limit / rollback Lua
├── src/main/resources/db/migration/
├── frontend/           # Vue 3 会场、订单、运营台
├── sql/seed.sql        # 演示账号与场次
├── docs/               # 各版本压测 / 联调报告
├── compose.yml
└── scripts/run-local.ps1
```

---

## 快速开始

### 环境

- JDK 21、Maven、Node.js 20+
- MySQL、Redis、RabbitMQ（或 Docker Compose）

### Docker

```powershell
copy .env.example .env
# 填入真实 DB / Redis / RabbitMQ / 支付宝参数后：
docker compose up --build
```

### 本地双实例 + 前端

1. 复制 `.env.example` 思路，使用仓库内 `.env.8080` / `.env.8081` 作为本机端口配置（**不要把含密钥的文件提交进 Git**）。
2. 启动中间件后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 8080
# 另一个终端：8081
cd frontend
npm install
npm run dev
```

3. 浏览器打开 `http://localhost:3000`。Vite 在代理目标为 8080/8081 时会去掉 `/api` 前缀。

Flyway 启动时执行 `V1__init_schema.sql` 等迁移。演示数据见 `sql/seed.sql`（会清空业务表）：

- 管理员 `admin` / `Admin@123456`
- 测试用户手机号如 `13800138001`，密码 `User@123456`

必须提供的环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`，以及支付宝 `ALIPAY_*`。Redis 无密码则不要设密码变量。生产 CORS 建议留空，由 Nginx 同源反代。

发布前应执行 `mvn verify`，并抽查：支付回调、MQ 失败重试、超时关单、Redis 与 MySQL 秒杀库存对账。

---

## 业务链路

**用户**：注册 / 登录 → 会场与详情（未开始 / 进行中 / 售罄 / 已结束）→ 验证码换 path → 提交抢购 → 轮询结果 → 待支付订单打开支付宝 → 异步 notify 入账。

**运营**：管理员登录（`Admin-Token`）→ 商品增删改 → 安全预热（不覆盖运行中库存）→ 待退款工单。

订单 `status`：`0` 待支付，`1` 已支付，`-1` 超时取消，`-2` 待退款。

---

## 测试与文档

- 后端：契约测试（Lua SQL 守卫、表唯一索引）、Mapper 注解断言、订单支付状态机单测；CI 跑 `./mvnw verify`。
- 前端：`npm run build` 含 `vue-tsc`。
- 历史证据：`docs/` 下 V0.5–V8.0 报告（含 JMeter 聚合图与联调截图）。

---

## License

教学与作品集用途。支付宝相关密钥仅使用沙箱，且不得提交到仓库。
