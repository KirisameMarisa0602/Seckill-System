package com.kirisamemarisa.seckillsystem.redis;

public class SeckillKey extends BasePrefix {
    private SeckillKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    public static SeckillKey getSeckillPath = new SeckillKey(60, "path");
    public static SeckillKey getSeckillCaptcha = new SeckillKey(60, "captcha");
}