package com.kirisamemarisa.seckillsystem.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.kirisamemarisa.seckillsystem.config.AlipayConfig;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.service.IOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/create/{orderId}")
    public String payOrder(@PathVariable Long orderId) {
        OrderInfo orderInfo = orderService.getById(orderId);
        if (orderInfo == null || orderInfo.getStatus() != 0) {
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
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getId().toString());
        bizContent.put("total_amount", orderInfo.getGoodsPrice().toString());
        bizContent.put("subject", "秒杀系统抢购: " + orderInfo.getGoodsName());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
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
            if (signVerified) {
                log.info("【支付宝异步回调】验签通过！交易号: {}", params.get("trade_no"));
                String tradeStatus = params.get("trade_status");
                if ("TRADE_SUCCESS".equals(tradeStatus)) {
                    String outTradeNo = params.get("out_trade_no");
                    Long orderId = Long.parseLong(outTradeNo);
                    orderService.paySuccess(orderId);
                }
                return "success";
            } else {
                log.error("【支付宝异步回调】⚠️ 验签失败！极有可能是恶意流量构造的支付回执！");
                return "failure";
            }
        } catch (Exception e) {
            log.error("处理支付宝回到事件出错", e);
            return "failure";
        }
    }
}