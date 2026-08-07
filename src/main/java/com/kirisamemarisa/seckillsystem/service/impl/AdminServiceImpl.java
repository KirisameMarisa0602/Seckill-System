package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.Admin;
import com.kirisamemarisa.seckillsystem.mapper.AdminMapper;
import com.kirisamemarisa.seckillsystem.redis.AdminKey;
import com.kirisamemarisa.seckillsystem.service.IAdminService;
import com.kirisamemarisa.seckillsystem.utils.MD5Util;
import com.kirisamemarisa.seckillsystem.vo.AdminLoginVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RespBean login(AdminLoginVo vo) {
        Admin admin = this.getOne(new QueryWrapper<Admin>().eq("username", vo.getUsername()));
        if (admin == null || !admin.getPassword().equals(MD5Util.md5(vo.getPassword()))) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(AdminKey.token.getPrefix() + token, admin.getUsername(), AdminKey.token.expireSeconds(), TimeUnit.SECONDS);

        return RespBean.success(token);
    }
}