package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.service.DeliveryAddressService;
import com.kirisamemarisa.seckillsystem.vo.DeliveryAddressRequest;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/address")
public class DeliveryAddressController {
    @Autowired private DeliveryAddressService deliveryAddressService;

    @GetMapping
    public RespBean current(User user) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        return RespBean.success(deliveryAddressService.findDefault(user.getId()));
    }

    @PostMapping
    public RespBean save(User user, @Valid @RequestBody DeliveryAddressRequest request) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.USER_NOT_EXIST);
        }
        return RespBean.success(deliveryAddressService.saveDefault(user.getId(), request));
    }
}
