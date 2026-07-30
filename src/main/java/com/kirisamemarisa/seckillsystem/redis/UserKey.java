package com.kirisamemarisa.seckillsystem.redis;

public class UserKey extends BasePrefix {
    private UserKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    //用户登录token维持7天
    public static UserKey token = new UserKey(3600 * 24 * 7,"token");
}