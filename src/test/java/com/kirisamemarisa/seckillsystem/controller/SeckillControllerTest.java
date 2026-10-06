package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.service.IGoodsService;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillControllerTest {
    @Mock private IGoodsService goodsService;
    @InjectMocks private SeckillController controller;

    @Test
    void missingGoodsIsNotReportedAsNotStarted() {
        when(goodsService.findGoodsVoByGoodsId(9L)).thenReturn(null);

        RespBean response = controller.doSeckill("path", user(), 9L);

        assertEquals(RespBeanEnum.GOODS_NOT_EXIST.getCode().longValue(), response.getCode());
    }

    @Test
    void futureWindowIsNotStarted() {
        GoodsVo goods = new GoodsVo();
        goods.setId(9L);
        goods.setSeckillPrice(new BigDecimal("1.00"));
        goods.setStartDate(LocalDateTime.now().plusDays(1));
        goods.setEndDate(LocalDateTime.now().plusDays(2));
        when(goodsService.findGoodsVoByGoodsId(9L)).thenReturn(goods);

        RespBean response = controller.doSeckill("path", user(), 9L);

        assertEquals(RespBeanEnum.SECKILL_NOT_START.getCode().longValue(), response.getCode());
    }

    @Test
    void goodsWithoutPriceIsIllegal() {
        GoodsVo goods = new GoodsVo();
        goods.setId(9L);
        goods.setStartDate(LocalDateTime.now().minusHours(1));
        goods.setEndDate(LocalDateTime.now().plusHours(1));
        when(goodsService.findGoodsVoByGoodsId(9L)).thenReturn(goods);

        RespBean response = controller.doSeckill("path", user(), 9L);

        assertEquals(RespBeanEnum.REQUEST_ILLEGAL.getCode().longValue(), response.getCode());
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        return user;
    }
}
