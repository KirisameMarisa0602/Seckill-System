package com.kirisamemarisa.seckillsystem.redis;

public class SeckillKey extends BasePrefix {
    private SeckillKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    //验证码和有效路径的生存期都是一分钟
    public static SeckillKey getSeckillPath = new SeckillKey(60, "path");
    public static SeckillKey getSeckillCaptcha = new SeckillKey(60, "captcha");
    public static SeckillKey outboxPending = new SeckillKey(0, "outboxPending");
    public static SeckillKey outboxEvent = new SeckillKey(0, "outboxEvent");
    public static SeckillKey outboxLock = new SeckillKey(30, "outboxLock");
}