package com.kirisamemarisa.seckillsystem.rabbitmq;

import com.kirisamemarisa.seckillsystem.config.RabbitMQConfig;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MQReceiver {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private IOrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void receive(SeckillMessage message) {
        log.info("【MQReceiver】从队列中拿到了一张订单，准备落库：{}", message);
        User user = message.getUser();
        Long goodsId = message.getGoodsId();
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goodsVo.getStockCount() < 1) {
            log.warn("【MQReceiver】手慢了，真实库存已售罄！商品ID：{}", goodsId);
            return; // 直接丢弃当前消息，不入库
        }
        try {
            orderService.createSeckillOrder(user, goodsVo);
            log.info("🎉【MQReceiver】订单真实落库成功：用户{}，商品{}", user.getId(), goodsId);
        } catch (Exception e) {
            log.error("【MQReceiver】订单异常：{}", e.getMessage());
        }
    }
}