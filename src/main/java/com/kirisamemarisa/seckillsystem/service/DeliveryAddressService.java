package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kirisamemarisa.seckillsystem.entity.DeliveryAddress;
import com.kirisamemarisa.seckillsystem.entity.OrderInfo;
import com.kirisamemarisa.seckillsystem.entity.OrderStatus;
import com.kirisamemarisa.seckillsystem.mapper.DeliveryAddressMapper;
import com.kirisamemarisa.seckillsystem.mapper.OrderInfoMapper;
import com.kirisamemarisa.seckillsystem.vo.DeliveryAddressRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryAddressService {
    @Autowired private DeliveryAddressMapper deliveryAddressMapper;

    @Autowired private OrderInfoMapper orderInfoMapper;

    public DeliveryAddress findDefault(Long userId) {
        return deliveryAddressMapper.selectOne(new QueryWrapper<DeliveryAddress>()
                .eq("user_id", userId)
                .eq("is_default", 1)
                .last("LIMIT 1"));
    }

    @Transactional(rollbackFor = Exception.class)
    public DeliveryAddress saveDefault(Long userId, DeliveryAddressRequest request) {
        DeliveryAddress address = findDefault(userId);
        if (address == null) {
            address = new DeliveryAddress();
            address.setUserId(userId);
            address.setIsDefault(1);
        }
        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(request.getReceiverPhone().trim());
        address.setDetail(request.getDetail().trim());
        if (address.getId() == null) {
            deliveryAddressMapper.insert(address);
        } else {
            deliveryAddressMapper.updateById(address);
        }
        orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .eq("user_id", userId)
                .eq("status", OrderStatus.UNPAID.code())
                .set("delivery_addr_id", address.getId())
                .set("receiver_name", address.getReceiverName())
                .set("receiver_phone", address.getReceiverPhone())
                .set("receiver_detail", address.getDetail()));
        return address;
    }
}
