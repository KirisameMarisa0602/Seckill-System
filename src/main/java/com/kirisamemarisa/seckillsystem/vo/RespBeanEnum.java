package com.kirisamemarisa.seckillsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum RespBeanEnum {
    SUCCESS(200, "SUCCESS"),
    ERROR(500, "服务端异常"),

    // ====== 登录模块错误码 ======
    LOGIN_ERROR(500210, "用户名或密码不正确"),
    MOBILE_FORMAT_ERROR(500211, "手机号码格式不正确"),
    BIND_ERROR(500212, "参数校验异常"),

    // ====== 秒杀模块错误码 ======
    EMPTY_STOCK(500200, "抱歉，库存不足！"),
    REPEAT_ERROR(500201, "该商品每人限购一件，请勿重复抢购！"),
    USER_NOT_EXIST(500202, "用户不存在或未登录"),
    // 👇👇新增下面这两个安全校验错误码👇👇
    REQUEST_ILLEGAL(500203, "请求非法或频繁，请重试"),
    CAPTCHA_ERROR(500204, "验证码错误，请重新输入");

    private final Integer code;
    private final String message;
}