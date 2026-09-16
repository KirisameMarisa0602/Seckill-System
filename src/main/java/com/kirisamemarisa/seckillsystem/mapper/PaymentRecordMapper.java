package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.PaymentRecord;

/**
 * {@code t_payment_record} Mapper。按 {@code trade_no} 查重走 {@code QueryWrapper}，无自定义 SQL。
 */
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
