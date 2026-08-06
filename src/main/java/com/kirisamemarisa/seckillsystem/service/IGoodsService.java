package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.AddGoodsVo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.UpdateGoodsVo;

import java.util.List;

public interface IGoodsService extends IService<Goods> {
    //给前端的所有商品视图对象
    List<GoodsVo> findGoodsVo();
    //根据ID找商品
    GoodsVo findGoodsVoByGoodsId(Long goodsId);
    //管理员上架商品
    RespBean addSeckillGoods(AddGoodsVo addGoodsVo);
    //管理员删除商品
    RespBean deleteSeckillGoods(Long goodsId);
    //管理员热更新商品信息
    RespBean updateSeckillGoods(UpdateGoodsVo updateGoodsVo);
    long countSeckillGoods();
    List<GoodsVo> findGoodsVoByLimit(int offset, int size);
}