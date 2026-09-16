/**
 * 进程内缓存。秒杀售罄标记放在 Caffeine，避免每次被打空的商品都穿透到 Redis。
 * 多节点通过 Redis Pub/Sub 频道 {@code stock_replenish_channel} 同步失效。
 */
package com.kirisamemarisa.seckillsystem.manager;
