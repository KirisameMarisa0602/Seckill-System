package com.kirisamemarisa.seckillsystem.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证订单行锁契约：支付与关单必须 {@code SELECT ... FOR UPDATE}，避免并发改同一订单。
 */
class OrderInfoMapperTest {

    /**
     * 断言 {@code selectByIdForUpdate} 的 SQL 含 {@code FOR UPDATE}。
     */
    @Test
    void paymentAndCancellationUseAPessimisticOrderRowLock() throws Exception {
        Select select = OrderInfoMapper.class
                .getMethod("selectByIdForUpdate", Long.class)
                .getAnnotation(Select.class);
        assertTrue(String.join(" ", select.value()).toUpperCase().contains("FOR UPDATE"));
    }
}
