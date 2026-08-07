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

    @GetMapping(value = "/create/{orderId}", produces = "text/html;charset=utf-8")
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
        log.warn("🚨 【生成付款链接】当前系统分配的回调地址是: {}", alipayConfig.getNotifyUrl());
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
        log.info("======================================================");
        log.info("📢 【绝密追踪】收到支付宝异步回调请求！正准备进入方法...");

        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = String.join(",", values);
            params.put(name, valueStr);
        }

        log.info("📦 【绝密追踪】支付宝传来的完整参数: {}", JSONObject.toJSONString(params));

        try {
            log.info("🛡️ 【绝密追踪】准备执行 RSA2 验签逻辑...");
            log.info("🔑 当前使用的支付宝公钥: {}", alipayConfig.getAlipayPublicKey());

            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );

            log.info("⚖️ 【绝密追踪】验签结果，大声告诉我: {}", signVerified);

            if (signVerified) {
                log.info("✅ 【绝密追踪】太棒了！验签通过！支付宝交易流水号: {}", params.get("trade_no"));
                String tradeStatus = params.get("trade_status");
                log.info("🔄 【绝密追踪】本次交易的状态是: {}", tradeStatus);

                if ("TRADE_SUCCESS".equals(tradeStatus)) {
                    String outTradeNo = params.get("out_trade_no");
                    Long orderId = Long.parseLong(outTradeNo);
                    log.info("💳 【绝密追踪】成功支付！准备执行数据库更改，目标订单ID: {}", orderId);

                    // ⚠️ 这里极其关键：把业务代码单独 Try，防止报 SQL 或者 Null 指针错误！
                    try {
                        orderService.paySuccess(orderId);
                        log.info("🎉 【绝密追踪】完美脱机！数据库订单状态修改指令已执行完成！");
                    } catch (Exception bizEx) {
                        log.error("💥 【绝密追踪】抓到内鬼了！业务代码执行 paySuccess 时发生了严重报错！", bizEx);
                        // 虽然业务报错，但钱收到了，依然要给支付宝返回 success，不然他们会以为你没收到消息，持续轰炸
                        return "success";
                    }
                }
                return "success";
            } else {
                log.error("❌ 【绝密追踪】验签居然失败了！！");
                log.error("👉 请重点检查 application.yml 的 alipayPublicKey！沙箱很容易填错成应用公钥！");
                return "fail";
            }
        } catch (Exception e) {
            log.error("💥 【绝密追踪】最外层爆发未知异常，可能是参数转换错乱导致！", e);
            return "fail";
        } finally {
            log.info("======================================================");
        }
    }
}