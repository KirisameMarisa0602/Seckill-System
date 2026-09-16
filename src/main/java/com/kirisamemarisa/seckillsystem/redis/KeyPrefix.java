package com.kirisamemarisa.seckillsystem.redis;

/**
 * Redis Key 前缀契约。业务写入 Redis 时必须声明 TTL 与前缀，避免各模块撞 key。
 *
 * <p>完整 key 由 {@link #getPrefix()} 再拼接业务标识组成；
 * {@link #expireSeconds()} 为 {@code 0} 表示前缀本身不设过期（调用方可另行指定）。
 */
public interface KeyPrefix {

    /**
     * @return 过期秒数；{@code 0} 表示不过期
     */
    int expireSeconds();

    /**
     * @return 统一前缀，格式为 {@code 类名:业务前缀:}
     */
    String getPrefix();
}
