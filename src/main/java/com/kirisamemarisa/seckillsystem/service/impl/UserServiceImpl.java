package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.mapper.UserMapper;
import com.kirisamemarisa.seckillsystem.redis.UserKey;
import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.utils.MD5Util;
import com.kirisamemarisa.seckillsystem.vo.LoginVo;
import com.kirisamemarisa.seckillsystem.vo.RegisterVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired private UserMapper userMapper;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RespBean doLogin(LoginVo loginVo) {
        String mobile = loginVo.getMobile();
        String pass = loginVo.getPassword();
        User user = userMapper.selectById(Long.valueOf(mobile));
        if (user == null || !MD5Util.formPassToDBPass(pass, user.getSalt()).equals(user.getPassword())) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setNickname(user.getNickname());
        safeUser.setHead(user.getHead());
        safeUser.setRegisterDate(user.getRegisterDate());
        redisTemplate.opsForValue().set(UserKey.token.getPrefix() + token, safeUser, UserKey.token.expireSeconds(), TimeUnit.SECONDS);
        return RespBean.success(token);
    }

    @Override
    public RespBean doRegister(RegisterVo registerVo) {
        String mobile = registerVo.getMobile();
        if (userMapper.selectById(Long.valueOf(mobile)) != null) { return RespBean.error(RespBeanEnum.MOBILE_HAS_REGISTERED); }
        User user = new User();
        user.setId(Long.valueOf(mobile));
        user.setNickname(registerVo.getNickname());
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        user.setSalt(salt);
        user.setPassword(MD5Util.formPassToDBPass(registerVo.getPassword(), salt));
        user.setHead("https://api.dicebear.com/7.x/avataaars/svg?seed=" + mobile);
        user.setRegisterDate(LocalDateTime.now());
        userMapper.insert(user);
        return RespBean.success("注册成功");
    }
}