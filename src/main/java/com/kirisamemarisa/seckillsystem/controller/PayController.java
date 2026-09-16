package com.kirisamemarisa.seckillsystem.controller;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.kirisamemarisa.seckillsystem.config.AlipayConfig;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import com.kirisamemarisa.seckillsystem.service.PaymentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pay")
public class PayController {
    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping(value = "/create/{orderId}", produces = "text/html;charset=utf-8")
    public String payOrder(User user, @PathVariable Long orderId) {
        if (user == null) {
            return "请先登录后再支付！";
        }
        OrderInfo orderInfo = orderService.getById(orderId);
        if (orderInfo == null || !user.getId().equals(orderInfo.getUserId())
                || !Integer.valueOf(0).equals(orderInfo.getStatus())) {
            return "订单非法或已脱离待支付状态！";
        }
        AlipayClient alipayClient = new DefaultAlipayClient(
                alipayConfig.getGatewayUrl(),
                alipayConfig.getAppId(),
                alipayConfig.getMerchantPrivateKey(),
                "json",
                "UTF-8",
                alipayConfig.getAlipayPublicKey(),
                "RSA2"
        );
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", orderInfo.getId().toString());
        bizContent.put("total_amount", orderInfo.getGoodsPrice().toString());
        bizContent.put("subject", "秒杀系统抢购: " + orderInfo.getGoodsName());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        try {
            request.setBizContent(objectMapper.writeValueAsString(bizContent));
            return alipayClient.pageExecute(request).getBody();
        } catch (Exception e) {
            log.error("生成支付宝付款链接报错！", e);
            return "调起支付失败！请稍后再试。";
        }
    }

    @PostMapping("/notify")
    public String payNotify(HttpServletRequest request) {
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = String.join(",", values);
            params.put(name, valueStr);
        }

        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
            if (!signVerified) {
                log.warn("支付宝异步通知验签失败");
                return "fail";
            }

            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return "success";
            }
            if (!alipayConfig.getAppId().equals(params.get("app_id"))
                    || (StringUtils.hasText(alipayConfig.getSellerId())
                    && !alipayConfig.getSellerId().equals(params.get("seller_id")))) {
                log.warn("支付宝异步通知商户身份校验失败");
                return "fail";
            }

            Long orderId = Long.valueOf(params.get("out_trade_no"));
            OrderInfo orderInfo = orderService.getById(orderId);
            BigDecimal paidAmount = new BigDecimal(params.get("total_amount"));
            if (orderInfo == null || orderInfo.getGoodsPrice() == null
                    || orderInfo.getGoodsPrice().compareTo(paidAmount) != 0) {
                log.warn("支付宝异步通知订单或金额校验失败，订单号: {}", orderId);
                return "fail";
            }

            PaymentResult result = orderService.paySuccess(
                    orderId,
                    params.get("trade_no"),
                    paidAmount,
                    params.get("app_id"),
                    params.get("seller_id")
            );
            if (result == PaymentResult.PAID || result == PaymentResult.ALREADY_PAID
                    || result == PaymentResult.REFUND_PENDING) {
                return "success";
            }
            return "fail";
        } catch (Exception e) {
            log.error("支付宝异步通知处理失败，将请求支付宝重试", e);
            return "fail";
        }
    }
}