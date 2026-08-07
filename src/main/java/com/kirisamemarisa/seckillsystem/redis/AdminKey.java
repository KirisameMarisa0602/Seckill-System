package com.kirisamemarisa.seckillsystem.redis;

public class AdminKey extends BasePrefix {
    private AdminKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    public static AdminKey token = new AdminKey(1800, "token");
}