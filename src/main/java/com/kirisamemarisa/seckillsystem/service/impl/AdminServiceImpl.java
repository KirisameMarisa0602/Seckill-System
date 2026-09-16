package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * {@link IAdminService} 实现。读写表 {@code t_admin}。
 * Token 存在 Redis，前缀见 {@link com.kirisamemarisa.seckillsystem.redis.AdminKey}。
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public RespBean login(AdminLoginVo vo) {
        Admin admin = this.getOne(new QueryWrapper<Admin>().eq("username", vo.getUsername()));
        if (admin == null || !passwordMatchesAndUpgrade(admin, vo.getPassword())) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(AdminKey.token.getPrefix() + token, admin.getUsername(), AdminKey.token.expireSeconds(), TimeUnit.SECONDS);

        return RespBean.success(token);
    }

    /**
     * 校验密码；旧 MD5 命中则立刻改写成 BCrypt。
     *
     * <p>{@code startsWith("$2")} 覆盖 {@code $2a$}/{@code $2b$}/{@code $2y$} 等 BCrypt 族前缀。
     * 管理员历史数据曾用无盐 MD5，匹配成功后 {@code updateById} 覆盖，下次只走 BCrypt。
     */
    private boolean passwordMatchesAndUpgrade(Admin admin, String submittedPassword) {
        String stored = admin.getPassword();
        if (stored != null && stored.startsWith("$2")) {
            return passwordEncoder.matches(submittedPassword, stored);
        }
        boolean matched = stored != null && stored.equals(MD5Util.md5(submittedPassword));
        if (matched) {
            admin.setPassword(passwordEncoder.encode(submittedPassword));
            this.updateById(admin);
        }
        return matched;
    }
}
