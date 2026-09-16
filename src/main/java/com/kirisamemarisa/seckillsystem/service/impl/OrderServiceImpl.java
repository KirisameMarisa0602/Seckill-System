package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.PaymentRecord;
import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.mapper.GoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.OrderInfoMapper;
import com.kirisamemarisa.seckillsystem.mapper.PaymentRecordMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillGoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
import com.kirisamemarisa.seckillsystem.redis.OrderKey;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.service.PaymentResult;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * {@link IOrderService} 实现。订单 {@code t_order}、一人一单 {@code t_seckill_order}、支付流水 {@code t_payment_record}。
 * 秒杀库存下单时预扣；主库存在支付成功时才扣。关单与支付回调靠 {@code FOR UPDATE} 互斥。
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderService {
    @Autowired private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired private SeckillOrderMapper seckillOrderMapper;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private GoodsMapper goodsMapper;

    @Autowired private PaymentRecordMapper paymentRecordMapper;

    /**
     * 创建秒杀订单：事务内预扣秒杀库存、插普通订单和一人一单行；成功后写 Redis 订单缓存。
     */
    @Override
    public OrderInfo createSeckillOrder(Long userId, GoodsVo goods) {
        OrderInfo orderInfo = transactionTemplate.execute(status -> {
            // WHERE stock_count > 0 的条件更新：影响行数 < 1 说明并发下已被抢光
            int updateRows = seckillGoodsMapper.decrementStock(goods.getId());
            if (updateRows < 1) { throw new RuntimeException("库存不足"); }
            OrderInfo info = new OrderInfo();
            info.setUserId(userId);
            info.setGoodsId(goods.getId());
            info.setDeliveryAddrId(0L);
            info.setGoodsName(goods.getGoodsName());
            info.setGoodsCount(1);
            info.setGoodsPrice(goods.getSeckillPrice());
            info.setOrderChannel(1);
            info.setStatus(0);
            info.setCreateDate(LocalDateTime.now());
            this.baseMapper.insert(info);
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setOrderId(info.getId());
            seckillOrder.setGoodsId(goods.getId());
            // (user_id, goods_id) 唯一索引：重复抢购在这里让整段事务回滚
            seckillOrderMapper.insert(seckillOrder);
            return info;
        });
        if (orderInfo != null) {
            redisTemplate.opsForValue().set(OrderKey.seckillOrderCache.getPrefix() + userId + ":" + goods.getId(),
                    orderInfo.getId(), OrderKey.seckillOrderCache.expireSeconds(), TimeUnit.SECONDS);
        }
        return orderInfo;
    }

    /**
     * 查该用户对该商品是否已有秒杀订单。无则返回 {@code null}。
     */
    @Override
    public Long findSeckillOrderId(Long userId, Long goodsId) {
        SeckillOrder order = seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>()
                .eq("user_id", userId)
                .eq("goods_id", goodsId)
                .last("LIMIT 1"));
        return order == null ? null : order.getOrderId();
    }

    /**
     * 超时关单：仅待支付单改为已取消，回补秒杀库存并清 Redis 一人一单标记。
     */
    @Override
    public void cancelTimeoutOrder(Long orderId) {
        final OrderInfo[] canceledOrder = new OrderInfo[1];
        Boolean isCanceled = transactionTemplate.execute(status -> {
            // FOR UPDATE 锁行，与 paySuccess 互斥，避免「一边关单一边入账」
            OrderInfo orderInfo = this.baseMapper.selectByIdForUpdate(orderId);
            if (orderInfo == null || !Integer.valueOf(0).equals(orderInfo.getStatus())) {
                return false;
            }
            orderInfo.setStatus(-1);
            this.baseMapper.updateById(orderInfo);
            // 删秒杀订单行，释放一人一单占用，用户可再抢
            seckillOrderMapper.delete(new QueryWrapper<SeckillOrder>().eq("order_id", orderId));
            // 只回补秒杀预扣库存；主库存支付前尚未扣除
            if(seckillGoodsMapper.selectOne(new QueryWrapper<SeckillGoods>().eq("goods_id", orderInfo.getGoodsId())) != null) {
                seckillGoodsMapper.incrementStock(orderInfo.getGoodsId());
            }
            canceledOrder[0] = orderInfo;
            return true;
        });
        if (Boolean.TRUE.equals(isCanceled)) {
            OrderInfo orderInfo = canceledOrder[0];
            stringRedisTemplate.opsForValue().increment(GoodsKey.getSeckillGoodsStock.getPrefix() + orderInfo.getGoodsId());
            stringRedisTemplate.delete(GoodsKey.isStockEmpty.getPrefix() + orderInfo.getGoodsId());
            stringRedisTemplate.convertAndSend("stock_replenish_channel", orderInfo.getGoodsId().toString());
            redisTemplate.delete(OrderKey.seckillOrderCache.getPrefix() + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
            stringRedisTemplate.delete(OrderKey.seckillUserOrder.getPrefix() + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
            log.info("====== [超时守护] 订单:{} 系统已安全关单并抛弃库存预留卡槽！======", orderId);
        }
    }

    /**
     * 支付宝异步通知入账。金额匹配且待支付则扣主库存并置已支付；关单后到账或主库存不足则待退款。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResult paySuccess(Long orderId, String tradeNo, BigDecimal amount,
                                    String appId, String sellerId) {
        // trade_no 唯一：同一笔支付宝通知重放时按已有流水返回，保证幂等
        PaymentRecord existing = paymentRecordMapper.selectOne(
                new QueryWrapper<PaymentRecord>().eq("trade_no", tradeNo));
        if (existing != null) {
            if (!existing.getOrderId().equals(orderId)) {
                return PaymentResult.INVALID_NOTIFICATION;
            }
            return "PAID".equals(existing.getStatus())
                    ? PaymentResult.ALREADY_PAID : PaymentResult.REFUND_PENDING;
        }

        OrderInfo orderInfo = this.baseMapper.selectByIdForUpdate(orderId);
        if (orderInfo == null) {
            return PaymentResult.ORDER_NOT_FOUND;
        }
        if (orderInfo.getGoodsPrice() == null || amount == null
                || orderInfo.getGoodsPrice().compareTo(amount) != 0) {
            return PaymentResult.INVALID_NOTIFICATION;
        }

        if (Integer.valueOf(1).equals(orderInfo.getStatus())) {
            savePaymentRecord(orderInfo, tradeNo, amount, appId, sellerId, "PAID");
            return PaymentResult.ALREADY_PAID;
        }

        // 非待支付（通常已超时取消）：改 -2 待退款，不能把钱当成有效成交
        if (!Integer.valueOf(0).equals(orderInfo.getStatus())) {
            orderInfo.setStatus(-2);
            this.baseMapper.updateById(orderInfo);
            savePaymentRecord(orderInfo, tradeNo, amount, appId, sellerId, "REFUND_PENDING");
            log.error("订单 {} 在取消后收到付款，已进入待退款工单状态", orderId);
            return PaymentResult.REFUND_PENDING;
        }

        // 主库存在支付成功时才扣；秒杀库存下单时已预扣。主库存没了则待退款
        if (goodsMapper.decrementGoodsStock(orderInfo.getGoodsId()) < 1) {
            orderInfo.setStatus(-2);
            this.baseMapper.updateById(orderInfo);
            savePaymentRecord(orderInfo, tradeNo, amount, appId, sellerId, "REFUND_PENDING");
            log.error("订单 {} 已付款但主库存不足，已进入待退款工单状态", orderId);
            return PaymentResult.REFUND_PENDING;
        }

        orderInfo.setStatus(1);
        orderInfo.setPayDate(LocalDateTime.now());
        this.baseMapper.updateById(orderInfo);
        savePaymentRecord(orderInfo, tradeNo, amount, appId, sellerId, "PAID");
        log.info("订单 {} 支付入账及主库存扣减在同一事务内完成", orderId);
        return PaymentResult.PAID;
    }

    /** 插入支付流水。{@code t_payment_record.trade_no} 唯一，并发双插会让本事务失败回滚。 */
    private void savePaymentRecord(OrderInfo orderInfo, String tradeNo, BigDecimal amount,
                                   String appId, String sellerId, String status) {
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(orderInfo.getId());
        record.setTradeNo(tradeNo);
        record.setAmount(amount);
        record.setAppId(appId);
        record.setSellerId(sellerId);
        record.setStatus(status);
        record.setCreateDate(LocalDateTime.now());
        record.setUpdateDate(LocalDateTime.now());
        paymentRecordMapper.insert(record);
    }
}
