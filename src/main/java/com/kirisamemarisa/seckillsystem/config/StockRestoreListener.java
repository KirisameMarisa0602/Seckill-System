package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.manager.LocalCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 库存恢复监听器。
 *
 * <p>订阅频道 {@code stock_replenish_channel}（在 {@link RedisConfig#container} 注册），
 * 收到商品 ID 后清除本进程 Caffeine 售罄标记，让补货后的请求重新走 Redis。
 * 依赖中间件：Redis。无 {@code @Order}，随容器启动订阅。
 */
@Slf4j
@Component
public class StockRestoreListener implements MessageListener {
    @Autowired
    private LocalCacheManager cacheManager;

    /**
     * 处理全服补货广播。消息体为商品 ID 字符串，Jackson 序列化时可能带引号，需剥掉后再解析。
     *
     * @param message Redis 原始消息
     * @param pattern 匹配到的频道模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            String goodsIdStr = body.replace("\"", "");
            Long goodsId = Long.valueOf(goodsIdStr);
            log.info("【Redis Pub/Sub】监听到全服广播，商品 {} 恢复了库存，准备通知解封！", goodsId);
            cacheManager.removeEmpty(goodsId);
        } catch (Exception e) {
            log.error("【Redis Pub/Sub】接收库存恢复消息解析失败", e);
        }
    }
}
