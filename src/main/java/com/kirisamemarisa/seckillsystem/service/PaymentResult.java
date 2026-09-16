package com.kirisamemarisa.seckillsystem.service;

/**
 * 支付回调处理结果。给 {@link IOrderService#paySuccess} 与支付宝 notify 使用，
 * 不是 HTTP 的 {@code RespBeanEnum}。
 */
public enum PaymentResult {
    /** 本次通知完成入账：订单 0→1，主库存已扣，流水 status=PAID。 */
    PAID,
    /** 重复通知或订单本就是已支付，应对支付宝返回 success，避免它一直重试。 */
    ALREADY_PAID,
    /** 关单后到账或主库存不足，订单 status=-2，流水 REFUND_PENDING，需人工退款。 */
    REFUND_PENDING,
    /** 按 out_trade_no 找不到 {@code t_order}。 */
    ORDER_NOT_FOUND,
    /** 金额与下单价不一致，或同一支付宝 trade_no 已绑定其他订单。 */
    INVALID_NOTIFICATION
}
