package com.kirisamemarisa.seckillsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝开放平台参数，绑定 {@code alipay.*}（可由环境变量覆盖）。
 *
 * <p>供 {@code PayController} 组装电脑网站支付请求与验签。密钥不下发前端。
 * 依赖外部支付宝网关；本地无 {@code @Order}。未配置网关/密钥时支付接口会失败，不影响秒杀下单主链路。
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {
    /** 支付宝网关地址，沙箱与正式环境不同。 */
    private String gatewayUrl;
    /** 应用 APPID。 */
    private String appId;
    /** 卖家支付宝账号（PID）。 */
    private String sellerId;
    /** 商户应用私钥，用于签名请求。 */
    private String merchantPrivateKey;
    /** 支付宝公钥，用于校验异步通知。 */
    private String alipayPublicKey;
    /** 支付结果异步通知 URL，需公网可达。 */
    private String notifyUrl;
    /** 用户付款后浏览器回跳地址。 */
    private String returnUrl;
}
