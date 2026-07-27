package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.Goods;
import com.kirisamemarisa.seckillsystem.vo.GoodsVo;
import java.util.List;

public interface IGoodsService extends IService<Goods> {
    List<GoodsVo> findGoodsVo();
    GoodsVo findGoodsVoByGoodsId(Long goodsId);
}