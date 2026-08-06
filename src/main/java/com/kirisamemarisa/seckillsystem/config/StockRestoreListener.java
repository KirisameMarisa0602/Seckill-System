package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.manager.LocalCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockRestoreListener implements MessageListener {
    @Autowired
    private LocalCacheManager cacheManager;

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