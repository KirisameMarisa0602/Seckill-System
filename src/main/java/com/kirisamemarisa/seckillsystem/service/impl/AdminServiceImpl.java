package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.Admin;
import com.kirisamemarisa.seckillsystem.mapper.AdminMapper;
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
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RespBean login(AdminLoginVo vo) {
        // 1. 去真实的 t_admin 表中寻找该账号
        Admin admin = this.getOne(new QueryWrapper<Admin>().eq("username", vo.getUsername()));
        if (admin == null) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        // 2. 密码比对：将前端明文进行MD5加密，对比数据库中存的密文
        if (!admin.getPassword().equals(MD5Util.md5(vo.getPassword()))) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        // 3. 登录成功，颁发专属 Token 到 Redis
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("admin:ticket:" + token, admin.getUsername(), 30, TimeUnit.MINUTES);

        return RespBean.success(token);
    }
}