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
    LOGIN_ERROR(500210, "用户名或密码不正确"),
    MOBILE_FORMAT_ERROR(500211, "手机号码格式不正确"),
    BIND_ERROR(500212, "参数校验异常"),
    EMPTY_STOCK(500200, "抱歉，库存不足！"),
    REPEAT_ERROR(500201, "该商品每人限购一件，请勿重复抢购！"),
    USER_NOT_EXIST(500202, "用户不存在或未登录"),
    REQUEST_ILLEGAL(500203, "请求非法或频繁，请重试"),
    CAPTCHA_ERROR(500204, "验证码错误，请重新输入"),
    ACCESS_LIMIT_REACHED(500205, "访问过于频繁，请稍后再试");
    //因为是规定的状态码枚举，final，不需更改，所以只需要@Getter注解
    private final Integer code;
    private final String message;
}