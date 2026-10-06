package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.OrderStatus;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.exception.GlobalException;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayControllerTest {
    @Mock private IOrderService orderService;
    @InjectMocks private PayController controller;

    @Test
    void unpaidOrderWithoutAddressCannotOpenCashier() {
        OrderInfo order = new OrderInfo();
        order.setId(8L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.UNPAID.code());
        when(orderService.getById(8L)).thenReturn(order);
        User user = new User();
        user.setId(1L);

        GlobalException error = assertThrows(GlobalException.class, () -> controller.payOrder(user, 8L));

        assertEquals(RespBeanEnum.ADDRESS_REQUIRED, error.getRespBeanEnum());
    }
}
