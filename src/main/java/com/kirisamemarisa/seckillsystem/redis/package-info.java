/**
 * Redis Key 前缀。统一格式为 {@code 类名:业务前缀:}，避免不同模块撞 key。
 * {@code expireSeconds() == 0} 表示不过期（如可售库存），其它如 Token、验证码带 TTL。
 */
package com.kirisamemarisa.seckillsystem.redis;
