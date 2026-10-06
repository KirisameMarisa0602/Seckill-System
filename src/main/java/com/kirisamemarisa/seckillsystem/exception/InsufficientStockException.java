package com.kirisamemarisa.seckillsystem.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException() {
        super("库存不足");
    }
}
