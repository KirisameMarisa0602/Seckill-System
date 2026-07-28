package com.kirisamemarisa.seckillsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum RespBeanEnum {
    // 通用
    SUCCESS(200, "SUCCESS"),
    ERROR(500, "服务端异常"),

    // 秒杀模块 5002xx
    EMPTY_STOCK(500200, "抱歉，库存不足！"),
    REPEAT_ERROR(500201, "该商品每人限购一件，请勿重复抢购！"),
    USER_NOT_EXIST(500202, "用户不存在或未登录");

    private final Integer code;
    private final String message;
}