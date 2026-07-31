package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.SeckillOrder;
import com.kirisamemarisa.seckillsystem.mapper.OrderInfoMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillGoodsMapper;
import com.kirisamemarisa.seckillsystem.mapper.SeckillOrderMapper;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderService {
    @Autowired private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired private SeckillOrderMapper seckillOrderMapper;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Transactional
    @Override
    public OrderInfo createSeckillOrder(Long userId, GoodsVo goods) {
        int updateRows = seckillGoodsMapper.decrementStock(goods.getId());
        if (updateRows < 1) { return null; }
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(userId);
        orderInfo.setGoodsId(goods.getId());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsName(goods.getGoodsName());
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsPrice(goods.getSeckillPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setCreateDate(new Date());
        this.baseMapper.insert(orderInfo);
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(userId);
        seckillOrder.setOrderId(orderInfo.getId());
        seckillOrder.setGoodsId(goods.getId());
        seckillOrderMapper.insert(seckillOrder);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTemplate.opsForValue().set("seckillOrderCache:" + userId + ":" + goods.getId(), orderInfo.getId(), 1, TimeUnit.HOURS);
                }
            });
        } else {
            redisTemplate.opsForValue().set("seckillOrderCache:" + userId + ":" + goods.getId(), orderInfo.getId(), 1, TimeUnit.HOURS);
        }
        return orderInfo;
    }

    @Transactional
    @Override
    public void cancelTimeoutOrder(Long orderId) {
        OrderInfo orderInfo = this.getById(orderId);
        if (orderInfo == null) { return; }
        boolean updated = this.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<OrderInfo>()
                .eq("id", orderId)
                .eq("status", 0)
                .set("status", -1));
        if (!updated) { return; }
        seckillOrderMapper.delete(new QueryWrapper<SeckillOrder>().eq("order_id", orderId));
        seckillGoodsMapper.incrementStock(orderInfo.getGoodsId());
        stringRedisTemplate.opsForValue().increment("seckillGoods:" + orderInfo.getGoodsId());
        redisTemplate.delete("seckillOrderCache:" + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
        stringRedisTemplate.delete("seckillUserOrder:" + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
        stringRedisTemplate.delete("isStockEmpty:" + orderInfo.getGoodsId());
        stringRedisTemplate.convertAndSend("stock_replenish_channel", orderInfo.getGoodsId().toString());
        System.out.println("====== [超时守护动作触发] 订单ID: " + orderId + " 未在1分钟内支付，系统已关单并成功回补所有库存与购买限额！======");
    }
}