package com.kirisamemarisa.seckillsystem.redis;

/**
 * Redis Key 前缀基类。子类只声明业务前缀与 TTL，拼接逻辑集中在 {@link #getPrefix()}。
 *
 * <p>完整前缀为 {@code 子类简单类名:业务前缀:}，例如 {@code GoodsKey:stock:}，用于隔离不同模块。
 */
public abstract class BasePrefix implements KeyPrefix {
    private int expireSeconds;
    private String prefix;

    /**
     * 构造不过期的前缀（{@code expireSeconds == 0}）。
     *
     * @param prefix 业务前缀片段
     */
    public BasePrefix(String prefix) {
        this(0, prefix);
    }

    /**
     * @param expireSeconds 过期秒数，{@code 0} 表示不过期
     * @param prefix        业务前缀片段
     */
    public BasePrefix(int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }

    @Override
    public int expireSeconds() {
        return expireSeconds;
    }

    /**
     * 拼出 {@code 类名:业务前缀:}，避免不同 Redis 键互相覆盖。
     */
    @Override
    public String getPrefix() {
        String className = getClass().getSimpleName();
        return className + ":" + prefix + ":";
    }
}
