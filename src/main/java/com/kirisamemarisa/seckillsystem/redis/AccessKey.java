package com.kirisamemarisa.seckillsystem.redis;

/**
 * 接口限流与 IP 黑名单 Redis Key。
 *
 * <p>完整前缀形如 {@code AccessKey:access:} / {@code AccessKey:blacklist:}。
 */
public class AccessKey extends BasePrefix {
    private AccessKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 按接口窗口构造限流计数 Key。
     * <p>前缀：{@code AccessKey:access:}；TTL：由 {@code expireSeconds} 决定（与 {@code @AccessLimit.second} 一致）。
     * <p>用途：Lua 对 {@code URI:userId} 或 {@code URI:ip} 计数，超出阈值拒绝请求。
     *
     * @param expireSeconds 滑动窗口秒数
     */
    public static AccessKey withExpire(int expireSeconds) {
        return new AccessKey(expireSeconds, "access");
    }

    /**
     * IP 黑名单。
     * <p>前缀：{@code AccessKey:blacklist:}；TTL：300 秒。
     * <p>用途：命中后拦截该 IP 的后续请求。
     */
    public static AccessKey blacklist = new AccessKey(300, "blacklist");
}
