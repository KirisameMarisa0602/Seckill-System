package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.controller.SeckillController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockRestoreListener implements MessageListener {

    @Autowired
    private SeckillController seckillController;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 读取从Redis广播通道中传过来的信件
            String body = new String(message.getBody());
            // 因为使用的是 Json 序列化器，发送的字符串被转义后可能会带上双引号，这里剔除它
            String goodsIdStr = body.replace("\"", "");
            Long goodsId = Long.valueOf(goodsIdStr);

            log.info("【Redis Pub/Sub】监听到全服广播，商品 {} 恢复了库存，准备通知Controller解除封禁！", goodsId);

            // 调用 Controller 开放该商品的本地缓存大门
            seckillController.clearEmptyStock(goodsId);
        } catch (Exception e) {
            log.error("【Redis Pub/Sub】接收库存恢复消息解析失败", e);
        }
    }
}