package com.kirisamemarisa.seckillsystem.redis;

public class AccessKey extends BasePrefix {
    private AccessKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    // 限流的时间是注解里动态传进来的，所以提供一个带有参数的方法
    public static AccessKey withExpire(int expireSeconds) {
        return new AccessKey(expireSeconds, "access");
    }
}