package com.kirisamemarisa.seckillsystem.redis;

public class UserKey extends BasePrefix {
    private UserKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    // Token 过期时间设置为 30 天 (3600秒 * 24小时 * 30天)
    public static UserKey token = new UserKey(3600 * 24 * 30, "tk");
}