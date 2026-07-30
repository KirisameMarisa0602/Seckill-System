//这是一个半成品类（abstract 表示它不能直接被 new 出来使用），它帮所有的子类把最累的活儿干了，子类不需要自己去写复杂的拼接逻辑，全部继承他的getPrefix即可
package com.kirisamemarisa.seckillsystem.redis;

public abstract class BasePrefix implements KeyPrefix {
    private int expireSeconds;
    private String prefix;

    //如果不传有效期，有效期默认为0，表示不过期
    public BasePrefix(String prefix) {
        this(0, prefix);
    }

    //正常传入有效期和前缀
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
        //格式为：
        //某种类型的redis数据:前缀:
        //解决了不同redis键值对命名冲突问题
        String className = getClass().getSimpleName();
        return className + ":" + prefix + ":";
    }
}