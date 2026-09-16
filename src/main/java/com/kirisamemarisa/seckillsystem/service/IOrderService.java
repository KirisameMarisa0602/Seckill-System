package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import java.math.BigDecimal;

/**
 * 订单与支付入账。对应 {@code t_order} / {@code t_seckill_order} / {@code t_payment_record}，
 * 并由 {@link com.kirisamemarisa.seckillsystem.service.impl.OrderServiceImpl} 实现。
 * 秒杀库存在下单时预扣，主库存（{@code t_goods.goods_stock}）在支付成功时才扣。
 */
public interface IOrderService extends IService<OrderInfo> {
    /**
     * 创建秒杀订单。事务内：预扣 {@code t_seckill_goods.stock_count} → 插 {@code t_order}（status=0）
     * → 插 {@code t_seckill_order}（一人一单唯一索引）。
     *
     * <p>库存不足抛 {@code RuntimeException}，整段回滚。成功后在事务外写 Redis 订单缓存。
     * 本方法不返回 {@code RespBean}，由 MQ 消费者调用。
     */
    OrderInfo createSeckillOrder(Long userId, GoodsVo goods);

    /**
     * 查该用户对该商品是否已有秒杀订单。无则返回 {@code null}，有则返回 {@code t_order} 主键。
     * 只读，无事务。
     */
    Long findSeckillOrderId(Long userId, Long goodsId);

    /**
     * 超时关单。事务内 {@code SELECT ... FOR UPDATE}，仅 {@code status=0} 才改为 {@code -1}，
     * 并删除秒杀订单行、回补秒杀库存。已支付/已取消直接跳过。
     * 提交后再回补 Redis 库存并清一人一单缓存。
     */
    void cancelTimeoutOrder(Long orderId);

    /**
     * 支付宝异步通知入账。整段 {@code @Transactional}。
     *
     * <ul>
     *   <li>{@link PaymentResult#PAID}：待支付单金额匹配，扣主库存并置 status=1</li>
     *   <li>{@link PaymentResult#ALREADY_PAID}：同一 trade_no 或订单已是已支付，幂等成功</li>
     *   <li>{@link PaymentResult#REFUND_PENDING}：关单后到账或主库存不足，status=-2，需人工退款</li>
     *   <li>{@link PaymentResult#ORDER_NOT_FOUND}：订单不存在</li>
     *   <li>{@link PaymentResult#INVALID_NOTIFICATION}：金额不符或 trade_no 绑了别的订单</li>
     * </ul>
     */
    PaymentResult paySuccess(Long orderId, String tradeNo, BigDecimal amount,
                             String appId, String sellerId);
}
