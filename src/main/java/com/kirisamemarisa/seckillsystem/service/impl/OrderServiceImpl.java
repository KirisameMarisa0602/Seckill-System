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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderService {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Transactional
    @Override
    public OrderInfo createSeckillOrder(Long userId, GoodsVo goods) {
        int updateRows = seckillGoodsMapper.decrementStock(goods.getId());
        if (updateRows < 1) {
            return null;
        }

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

        // ==========================================
        // 架构升级 A：利用事务同步器，保证数据库事务与Redis缓存的最终一致性
        // ==========================================
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 只有当上的 insert 等 DB 操作彻底 COMMIT 落盘后，才会执行到这里！
                    redisTemplate.opsForValue().set(
                            "seckillOrderCache:" + userId + ":" + goods.getId(),
                            orderInfo.getId(),
                            1, TimeUnit.HOURS
                    );
                }
            });
        } else {
            // 如果处于非事务环境中的容错兜底，直接写内存
            redisTemplate.opsForValue().set(
                    "seckillOrderCache:" + userId + ":" + goods.getId(),
                    orderInfo.getId(),
                    1, TimeUnit.HOURS
            );
        }

        return orderInfo;
    }

    /**
     * 【核心兜底逻辑】：超时关单，释放全部资源
     */
    @Transactional
    @Override
    public void cancelTimeoutOrder(Long orderId) {
        OrderInfo orderInfo = this.getById(orderId);
        // 1. 幂等与状态校验：如果订单不存在，或者状态不等于 0 (未支付)，说明用户已经支付或单据已做处理，不用关单
        if (orderInfo == null || orderInfo.getStatus() != 0) {
            return;
        }

        // 2. 修改普通订单状态为 -1 (已取消)
        orderInfo.setStatus(-1);
        this.updateById(orderInfo);

        // 3. 移除秒杀订单凭证，让该用户有再抢一次的机会
        seckillOrderMapper.delete(
                new QueryWrapper<SeckillOrder>().eq("order_id", orderId)
        );

        // 4. 回补 MySQL 中的秒杀商品库存
        seckillGoodsMapper.incrementStock(orderInfo.getGoodsId());

        // 5. 回补 Redis 中的库存，并清理该用户抢到过商品的限购缓存标记
        redisTemplate.opsForValue().increment("seckillGoods:" + orderInfo.getGoodsId());
        redisTemplate.delete("seckillOrderCache:" + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
        redisTemplate.delete("isStockEmpty:" + orderInfo.getGoodsId());

        System.out.println("====== [超时守护动作触发] 订单ID: " + orderId + " 未在1分钟内支付，系统已关单并成功回补所有库存与购买限额！======");
    }
}