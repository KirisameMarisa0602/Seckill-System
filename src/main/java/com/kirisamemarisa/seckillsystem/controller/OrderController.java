package com.kirisamemarisa.seckillsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private IOrderService orderService;

    @GetMapping("/list")
    @ResponseBody
    public RespBean getOrderList(User user) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        List<OrderInfo> orderList = orderService.list(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getUserId, user.getId())
                        .orderByDesc(OrderInfo::getCreateDate)
        );
        return RespBean.success(orderList);
    }

    @GetMapping("/detail/{orderId}")
    @ResponseBody
    public RespBean getOrderDetail(User user, @PathVariable Long orderId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        OrderInfo orderInfo = orderService.getById(orderId);
        if (orderInfo == null || !orderInfo.getUserId().equals(user.getId())) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        return RespBean.success(orderInfo);
    }
}