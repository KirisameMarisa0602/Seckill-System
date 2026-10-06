package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.AddGoodsVo;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.UpdateGoodsVo;

import java.util.List;

public interface IGoodsService extends IService<Goods> {

    List<GoodsVo> findGoodsVo();

    GoodsVo findGoodsVoByGoodsId(Long goodsId);

    RespBean addSeckillGoods(AddGoodsVo addGoodsVo);

    RespBean deleteSeckillGoods(Long goodsId);

    RespBean updateSeckillGoods(UpdateGoodsVo updateGoodsVo);

    long countSeckillGoods();

    List<GoodsVo> findGoodsVoByLimit(int offset, int size);
}
