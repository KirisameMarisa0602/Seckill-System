package com.kirisamemarisa.seckillsystem.entity;

public enum OrderStatus {
    REFUND_PENDING(-2),
    CANCELED(-1),
    UNPAID(0),
    PAID(1);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean same(Integer value) {
        return value != null && value == code;
    }
}
