package com.kirisamemarisa.seckillsystem.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessLimit {
    int second();     // 时间窗口（秒）
    int maxCount();   // 最大访问次数
    boolean needLogin() default true; // 是否必须登录才能访问
}