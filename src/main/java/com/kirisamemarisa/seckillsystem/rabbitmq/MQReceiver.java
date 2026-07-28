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

/**
 * 消息接收者 (后厨机器人)
 */
@Service
@Slf4j
public class MQReceiver {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private IOrderService orderService;

    /**
     * 核心注解 @RabbitListener：只要程序启动，这个方法就会死死盯着 seckillQueue 队列。
     * 当有消息进来时，Spring 的消息转换器会自动把 JSON 转回 SeckillMessage 对象！
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void receive(SeckillMessage message) {
        // 【加入这行代码：让系统模拟每处理一个订单需要 1 秒钟】
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("【后厨机器人】从队列中拿到了一张小票，准备炒菜(落库)：{}", message);

        // 1. 拆开小票，看看是谁在买，买什么
        User user = message.getUser();
        Long goodsId = message.getGoodsId();

        // 2. 去数据库查一下当前商品最新的状态
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);

        // 3. 做最后一道防线：判断数据库库存是否大于0（虽然前方 Redis 已经挡过了，但后厨必须再确认一次）
        if (goodsVo.getStockCount() < 1) {
            log.warn("【后厨兜底拦截】手慢了，真实库存已售罄！商品ID：{}", goodsId);
            return; // 直接丢弃当前消息，不入库
        }

        /*
         * 4. 还能继续扩展：判断是否重复抢购（判断 t_seckill_order 是否已有该 userId 和 goodsId）
         * 注：你之前在 OrderServiceImpl 的注释写得很棒，依赖了 MySQL 唯一索引兜底，
         * 如果在此处发生并发插入，数据库会报 DuplicateKeyException 异常阻止超卖。
         */

        // 5. 调用核心业务逻辑：真刀真枪地减 MySQL 库存，生成订单！
        // 如果这里数据库抛出异常，RabbitMQ 默认机制会进行处理（根据配置可重试或抛弃）
        try {
            orderService.createSeckillOrder(user, goodsVo);
            log.info("🎉【后厨机器人】成功做出一个汉堡！订单真实落库成功：用户{}，商品{}", user.getId(), goodsId);
        } catch (Exception e) {
            log.error("【后厨机器人】炒菜失败（可能触发了唯一索引防重复购买，或数据库异常）：{}", e.getMessage());
        }
    }
}