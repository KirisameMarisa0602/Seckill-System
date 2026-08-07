package com.kirisamemarisa.seckillsystem.redis;

public class AccessKey extends BasePrefix {
    private AccessKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    public static AccessKey withExpire(int expireSeconds) {
        return new AccessKey(expireSeconds, "access");
    }
    public static AccessKey blacklist = new AccessKey(300, "blacklist");
}