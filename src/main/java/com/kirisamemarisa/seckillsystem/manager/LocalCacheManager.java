package com.kirisamemarisa.seckillsystem.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LocalCacheManager {
    private final Cache<Long, Boolean> emptyStockCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    public void putEmpty(Long goodsId) { emptyStockCache.put(goodsId, true); }
    public Boolean checkEmpty(Long goodsId) { return emptyStockCache.getIfPresent(goodsId); }
    public void removeEmpty(Long goodsId) {
        emptyStockCache.invalidate(goodsId);
        log.info("【本地缓存防线同步】成功清空商品 {} 的本地售价空标记！", goodsId);
    }
}