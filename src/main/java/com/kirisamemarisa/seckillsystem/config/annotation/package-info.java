/**
 * 自定义注解。目前仅有 {@link com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit}，
 * 由 {@link com.kirisamemarisa.seckillsystem.config.AccessLimitInterceptor} 读取后走 Redis Lua 限流。
 */
package com.kirisamemarisa.seckillsystem.config.annotation;
