package com.kirisamemarisa.seckillsystem.redis;

/**
 * 秒杀订单 Redis Key。
 *
 * <p>完整前缀形如 {@code OrderKey:seckillOrderCache:} / {@code OrderKey:seckillUserOrder:}，
 * 再拼接 {@code userId:goodsId}。
 */
public class OrderKey extends BasePrefix {
    private OrderKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 已落库秒杀订单 ID 缓存。
     * <p>前缀：{@code OrderKey:seckillOrderCache:}；TTL：900 秒（15 分钟，与支付超时窗口一致）。
     * <p>用途：轮询 {@code /seckill/result} 时返回订单号，避免每次查库。
     */
    public static OrderKey seckillOrderCache = new OrderKey(900, "seckillOrderCache");

    /**
     * 用户-商品预占标记。
     * <p>前缀：{@code OrderKey:seckillUserOrder:}；Key 类 TTL 为不过期，Lua 实际按活动剩余秒数设置 {@code EX}。
     * <p>用途：预扣库存时写入，防止同一用户重复秒杀；关单或错误死信回滚时删除。
     */
    public static OrderKey seckillUserOrder = new OrderKey(0, "seckillUserOrder");
}
