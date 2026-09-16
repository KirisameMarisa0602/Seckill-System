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

/**
 * 登录用户的订单查询入口，对应前端 {@code orderApi.list}（OrdersView）。
 *
 * <p>处于秒杀链路的下单之后、支付前后：MQ 消费者落库成功后，用户在此查看待支付/已支付订单。
 * {@code User} 由拦截器 + 参数解析器从请求头 {@code token} 注入，不是 JSON 字段。
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private IOrderService orderService;

    /**
     * 当前用户订单列表，对应 {@code GET /order/list}。
     *
     * @param user     当前登录用户，未登录时为 {@code null}
     * @param page     页码；与 {@code pageSize} 都为空时返回该用户全量订单
     * @param pageSize 每页条数，缺省 20，上限 100
     * @return 未登录返回用户不存在；成功时 {@code obj} 为订单列表或分页结果
     * @implNote 只读订单表，按创建时间倒序
     */
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

    /**
     * 当前用户的单笔订单详情，对应 {@code GET /order/detail/{orderId}}。
     * 前端目前以列表为主，本接口用于按订单号核验归属。
     *
     * @param user    当前登录用户
     * @param orderId 路径变量，订单主键
     * @return 属于本人则返回 {@link OrderInfo}；否则非法请求
     * @implNote 只读订单表，并校验 {@code userId} 防止越权
     */
    @GetMapping("/detail/{orderId}")
    @ResponseBody
    public RespBean getOrderDetail(User user, @PathVariable Long orderId) { // {orderId} 从 URL 路径绑定
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
