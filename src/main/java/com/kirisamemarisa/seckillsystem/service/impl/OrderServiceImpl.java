package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.SeckillGoods;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.mapper.GoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.OrderInfoMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillGoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.rabbitmq.MQSender;
import com.kirisamemarisa.seckillsystem.redis.GoodsKey;
import com.kirisamemarisa.seckillsystem.redis.OrderKey;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderService {
    @Autowired private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired private SeckillOrderMapper seckillOrderMapper;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private GoodsMapper goodsMapper;

    @Autowired private MQSender mqSender;

    @Override
    public OrderInfo createSeckillOrder(Long userId, GoodsVo goods) {
        OrderInfo orderInfo = transactionTemplate.execute(status -> {
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
            seckillOrderMapper.insert(seckillOrder);
            return info;
        });
        if (orderInfo != null) {
            redisTemplate.opsForValue().set(OrderKey.seckillOrderCache.getPrefix() + userId + ":" + goods.getId(),
                    orderInfo.getId(), OrderKey.seckillOrderCache.expireSeconds(), TimeUnit.SECONDS);
        }
        return orderInfo;
    }

    @Override
    public void cancelTimeoutOrder(Long orderId) {
        OrderInfo orderInfo = this.getById(orderId);
        if (orderInfo == null) { return; }
        Boolean isCanceled = transactionTemplate.execute(status -> {
            boolean updated = this.update(new UpdateWrapper<OrderInfo>().eq("id", orderId).eq("status", 0).set("status", -1));
            if (!updated) { return false; }
            seckillOrderMapper.delete(new QueryWrapper<SeckillOrder>().eq("order_id", orderId));
            if(seckillGoodsMapper.selectOne(new QueryWrapper<SeckillGoods>().eq("goods_id", orderInfo.getGoodsId())) != null) {
                seckillGoodsMapper.incrementStock(orderInfo.getGoodsId());
            }
            return true;
        });
        if (Boolean.TRUE.equals(isCanceled)) {
            stringRedisTemplate.opsForValue().increment(GoodsKey.getSeckillGoodsStock.getPrefix() + orderInfo.getGoodsId());
            stringRedisTemplate.delete(GoodsKey.isStockEmpty.getPrefix() + orderInfo.getGoodsId());
            stringRedisTemplate.convertAndSend("stock_replenish_channel", orderInfo.getGoodsId().toString());
            redisTemplate.delete(OrderKey.seckillOrderCache.getPrefix() + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
            stringRedisTemplate.delete(OrderKey.seckillUserOrder.getPrefix() + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
            log.info("====== [超时守护] 订单:{} 系统已安全关单并抛弃库存预留卡槽！======", orderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean paySuccess(Long orderId) {
        OrderInfo orderInfo = this.getById(orderId);
        if (orderInfo == null) { return false; }
        boolean updated = this.update(new UpdateWrapper<OrderInfo>()
                .eq("id", orderId).eq("status", 0)
                .set("status", 1).set("pay_date", LocalDateTime.now()));
        if (!updated) return true;
        try {
            if (goodsMapper.decrementGoodsStock(orderInfo.getGoodsId()) > 0) {
                log.info("【资产流转】订单 {} 核爆完成，主商铺库存落地剥离", orderId);
            } else {
                throw new RuntimeException("【一致性预警】数据库普通库存严重不足");
            }
        } catch (Exception e) {
            mqSender.sendCompensateMessage(orderId);
        }
        return true;
    }
}