package com.kirisamemarisa.seckillsystem.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口访问频控注解，由 {@link com.kirisamemarisa.seckillsystem.config.AccessLimitInterceptor} 在 Handler 执行前读取。
 *
 * <p>计数落在 Redis，经 {@code rate-limit.lua} 原子 incr + expire。
 * {@code needLogin = true} 时按用户 ID 限流；匿名接口按客户端 IP 限流。
 * 依赖中间件：Redis。无独立启动顺序。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessLimit {
    /**
     * 限流时间窗口，单位秒；同时作为 Redis 计数 Key 的 TTL。
     */
    int second();

    /**
     * 窗口内允许的最大访问次数，超出返回 HTTP 429。
     */
    int maxCount();

    /**
     * 是否必须已登录。{@code true} 时未带有效 {@code token} 直接 401，且限流 Key 拼接用户 ID。
     */
    boolean needLogin() default true;
}
