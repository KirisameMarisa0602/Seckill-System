package com.kirisamemarisa.seckillsystem.redis;

public class GoodsKey extends BasePrefix {
    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    public static GoodsKey getGoodsVo = new GoodsKey(3600, "goodsVo");
    public static GoodsKey getSeckillGoodsStock = new GoodsKey(0, "stock");
    public static GoodsKey isStockEmpty = new GoodsKey(0, "empty");
}