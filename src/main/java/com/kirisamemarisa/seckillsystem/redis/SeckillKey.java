package com.kirisamemarisa.seckillsystem.redis;

public class SeckillKey extends BasePrefix {
    private SeckillKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    //验证码和有效路径的生存期都是一分钟
    public static SeckillKey getSeckillPath = new SeckillKey(60, "path");
    public static SeckillKey getSeckillCaptcha = new SeckillKey(60, "captcha");
}