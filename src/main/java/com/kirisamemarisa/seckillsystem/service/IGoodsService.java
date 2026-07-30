package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.AddGoodsVo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.UpdateGoodsVo;

import java.util.List;

public interface IGoodsService extends IService<Goods> {
    List<GoodsVo> findGoodsVo();
    GoodsVo findGoodsVoByGoodsId(Long goodsId);

    // 新增：B端后台上架秒杀商品
    RespBean addSeckillGoods(AddGoodsVo addGoodsVo);
    // 新增：B端联动下架秒杀商品
    RespBean deleteSeckillGoods(Long goodsId);
    RespBean updateSeckillGoods(UpdateGoodsVo updateGoodsVo);
}