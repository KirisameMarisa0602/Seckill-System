package com.kirisamemarisa.seckillsystem.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * 进程内售罄标记缓存（Caffeine）。
 *
 * <p>秒杀热路径在访问 Redis 前先查本地空库存标记，避免已售罄商品打穿 Redis。
 * 多节点通过 Redis 频道 {@code stock_replenish_channel} 通知 {@link #removeEmpty(Long)} 失效。
 */
@Slf4j
@Component
public class LocalCacheManager {
    /** 商品 ID → 已售罄；最多 1 万条，1 小时未访问淘汰。 */
    private final Cache<Long, Boolean> emptyStockCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    /**
     * 标记商品本地售罄，后续秒杀请求可直接拒绝。
     */
    public void putEmpty(Long goodsId) { emptyStockCache.put(goodsId, true); }

    /**
     * @return {@code true} 表示本地认为已售罄；{@code null} 表示无标记
     */
    public Boolean checkEmpty(Long goodsId) { return emptyStockCache.getIfPresent(goodsId); }

    /**
     * 清除本地售罄标记（库存回补或全服广播后调用）。
     */
    public void removeEmpty(Long goodsId) {
        emptyStockCache.invalidate(goodsId);
        log.info("【本地缓存防线同步】成功清空商品 {} 的本地售价空标记！", goodsId);
    }
}
