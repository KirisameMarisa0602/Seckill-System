package com.kirisamemarisa.seckillsystem.redis;

public abstract class BasePrefix implements KeyPrefix {
    private int expireSeconds;
    private String prefix;

    public BasePrefix(String prefix) {
        this(0, prefix); // 默认0永不过期
    }
    public BasePrefix(int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }
    @Override
    public int expireSeconds() {
        return expireSeconds;
    }
    @Override
    public String getPrefix() {
        // className + ":" + prefix + ":"  (例如 UserKey:tk:)
        String className = getClass().getSimpleName();
        return className + ":" + prefix + ":";
    }
}