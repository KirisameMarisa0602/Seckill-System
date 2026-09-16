package com.kirisamemarisa.seckillsystem.redis;

/**
 * 秒杀路径、验证码与 Outbox Redis Key。
 *
 * <p>路径/验证码带短 TTL；Outbox 待投递集合与事件体不过期，由发布器成功投递后删除。
 */
public class SeckillKey extends BasePrefix {
    private SeckillKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 秒杀隐藏路径。
     * <p>前缀：{@code SeckillKey:path:}；TTL：60 秒。
     * <p>用途：校验码通过后写入，{@code /{path}/doSeckill} 必须匹配该值。
     */
    public static SeckillKey getSeckillPath = new SeckillKey(60, "path");

    /**
     * 秒杀算术验证码答案。
     * <p>前缀：{@code SeckillKey:captcha:}；TTL：60 秒。
     * <p>用途：获取隐藏路径前核对用户输入。
     */
    public static SeckillKey getSeckillCaptcha = new SeckillKey(60, "captcha");

    /**
     * Outbox 待投递事件有序集合（ZSET）。
     * <p>前缀：{@code SeckillKey:outboxPending:}（无后缀，整 key 即该 ZSET）；TTL：不过期。
     * <p>用途：member 为 eventId，score 为可投递时间戳；Lua 预扣库存时写入，发布器按 score 扫描。
     */
    public static SeckillKey outboxPending = new SeckillKey(0, "outboxPending");

    /**
     * Outbox 事件载荷（HASH）。
     * <p>前缀：{@code SeckillKey:outboxEvent:}；TTL：不过期。
     * <p>用途：拼接 eventId 后存放 userId、goodsId、价格等字段，投递成功后删除。
     */
    public static SeckillKey outboxEvent = new SeckillKey(0, "outboxEvent");

    /**
     * Outbox 单事件发布锁。
     * <p>前缀：{@code SeckillKey:outboxLock:}；TTL：30 秒。
     * <p>用途：多实例发布同一 eventId 时互斥，防止重复投递。
     */
    public static SeckillKey outboxLock = new SeckillKey(30, "outboxLock");
}
