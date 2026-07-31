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

    @Transactional
    @Override
    public void cancelTimeoutOrder(Long orderId) {
        // 先查出订单，主要是为了拿到商品ID和用户ID，方便后续回补库存
        OrderInfo orderInfo = this.getById(orderId);
        if (orderInfo == null) {
            return; // 连订单都没有，直接结束
        }

        // ==========================================
        // 【核心修复】：利用 UpdateWrapper 执行原子更新，防止 ABA 漏洞
        // 对应的 SQL: UPDATE t_order SET status = -1 WHERE id = #{orderId} AND status = 0
        // 利用数据库原生行级锁，只有在它依然是未支付状态(0)时，才能将它成功改为已取消(-1)
        // ==========================================
        boolean updated = this.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<OrderInfo>()
                .eq("id", orderId)
                .eq("status", 0)       // 条件：目前必须是0（未支付）
                .set("status", -1));   // 更新：设为-1（已取消）

        // 如果 updated 为 false，说明状态已经不是 0 了（大概率是死信触发时，用户恰好支付成功变为了 1），此时坚决不能释放库存！直接结束。
        if (!updated) {
            return;
        }

        // 3. 移除秒杀订单凭证，让该用户有再抢一次的机会
        seckillOrderMapper.delete(
                new QueryWrapper<SeckillOrder>().eq("order_id", orderId)
        );

        // 4. 回补 MySQL 中的秒杀商品库存
        seckillGoodsMapper.incrementStock(orderInfo.getGoodsId());

        // 5. 回补 Redis 中的库存，并清理该用户抢到过商品的限购缓存标记
        redisTemplate.opsForValue().increment("seckillGoods:" + orderInfo.getGoodsId());
        redisTemplate.delete("seckillOrderCache:" + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
        redisTemplate.delete("seckillUserOrder:" + orderInfo.getUserId() + ":" + orderInfo.getGoodsId());
        redisTemplate.delete("isStockEmpty:" + orderInfo.getGoodsId());
        redisTemplate.convertAndSend("stock_replenish_channel", orderInfo.getGoodsId().toString());

        System.out.println("====== [超时守护动作触发] 订单ID: " + orderId + " 未在1分钟内支付，系统已关单并成功回补所有库存与购买限额！======");
    }
}