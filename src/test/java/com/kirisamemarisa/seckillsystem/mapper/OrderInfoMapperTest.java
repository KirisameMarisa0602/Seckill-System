package com.kirisamemarisa.seckillsystem.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderInfoMapperTest {
    @Test
    void paymentAndCancellationUseAPessimisticOrderRowLock() throws Exception {
        Select select = OrderInfoMapper.class
                .getMethod("selectByIdForUpdate", Long.class)
                .getAnnotation(Select.class);
        assertTrue(String.join(" ", select.value()).toUpperCase().contains("FOR UPDATE"));
    }
}
