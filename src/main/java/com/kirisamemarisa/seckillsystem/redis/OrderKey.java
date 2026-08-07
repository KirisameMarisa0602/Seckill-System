package com.kirisamemarisa.seckillsystem.redis;

public class OrderKey extends BasePrefix {
    private OrderKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    public static OrderKey seckillOrderCache = new OrderKey(900, "seckillOrderCache");
    public static OrderKey seckillUserOrder = new OrderKey(0, "seckillUserOrder");
}