package com.kirisamemarisa.seckillsystem.redis;

/**
 * 商品相关 Redis Key。
 *
 * <p>完整前缀形如 {@code GoodsKey:业务前缀:}，再拼接 {@code goodsId}。
 */
public class GoodsKey extends BasePrefix {
    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 单品详情 {@code GoodsVo} 缓存。
     * <p>前缀：{@code GoodsKey:goodsVo:}；TTL：3600 秒。
     * <p>用途：减轻商品详情读库；后台改价/改库存后需主动失效。
     */
    public static GoodsKey getGoodsVo = new GoodsKey(3600, "goodsVo");

    /**
     * 秒杀可售库存计数器。
     * <p>前缀：{@code GoodsKey:stock:}；TTL：不过期。
     * <p>用途：Lua 预扣/回补的原子计数；值为当前可售件数。
     */
    public static GoodsKey getSeckillGoodsStock = new GoodsKey(0, "stock");

    /**
     * 售罄标记。
     * <p>前缀：{@code GoodsKey:empty:}；TTL：不过期。
     * <p>用途：库存扣至 0 后写入 {@code "1"}，配合本地 Caffeine 拦截后续请求。
     */
    public static GoodsKey isStockEmpty = new GoodsKey(0, "empty");
}
