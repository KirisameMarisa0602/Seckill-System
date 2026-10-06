package com.kirisamemarisa.seckillsystem.exception;

import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void stockExceptionUsesEmptyStockCode() {
        RespBean response = handler.handleInsufficientStock(new InsufficientStockException());
        assertEquals(RespBeanEnum.EMPTY_STOCK.getCode().longValue(), response.getCode());
    }

    @Test
    void wrappedStockExceptionIsNotAGenericServerError() {
        RespBean response = handler.ExceptionHandler(new IllegalStateException("tx", new InsufficientStockException()));
        assertEquals(RespBeanEnum.EMPTY_STOCK.getCode().longValue(), response.getCode());
    }
}
