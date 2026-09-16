package com.kirisamemarisa.seckillsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import com.kirisamemarisa.seckillsystem.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private IOrderService orderService;

    @GetMapping("/list")
    @ResponseBody
    public RespBean getOrderList(User user,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer pageSize) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, user.getId())
                .orderByDesc(OrderInfo::getCreateDate);
        if (page == null && pageSize == null) {
            return RespBean.success(orderService.list(query));
        }
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        Page<OrderInfo> result = orderService.page(new Page<>(safePage, safePageSize), query);
        return RespBean.success(new PageResult<>(
                result.getTotal(), safePage, safePageSize, result.getRecords()));
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