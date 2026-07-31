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

//控制反转
@Service
//extends ServiceImpl<UserMapper, User>表示要操作的数据库表映射类是 UserMapper，里面的数据装在 User 这个实体类，通过Mybatis-Plus实现ORM
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    //依赖注入
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RespBean doLogin(LoginVo loginVo) {
        String mobile = loginVo.getMobile();
        //双重MD5加密1：（前端用户明文密码+静态盐）进行第一次MD5加密得到pass
        String pass = loginVo.getPassword();
        User user = userMapper.selectById(Long.valueOf(mobile));

        //如果没这个用户
        if (user == null) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        //双重MD5加密2：（pass+用户专属动态盐）进行第二次MD5加密得到落实到MySQL数据库里的用户密码
        String calcPass = MD5Util.formPassToDBPass(pass, user.getSalt());

        //如果密码不对
        if (!calcPass.equals(user.getPassword())) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        //密码正确颁发标识用户登陆状态的token
        String token = UUID.randomUUID().toString().replace("-", "");

        //存进redis
        redisTemplate.opsForValue().set(UserKey.token.getPrefix() + token, user, UserKey.token.expireSeconds(), TimeUnit.SECONDS);
        return RespBean.success(token);
    }

    @Override
    public RespBean doRegister(RegisterVo registerVo) {
        String mobile = registerVo.getMobile();
        User existUser = userMapper.selectById(Long.valueOf(mobile));
        if (existUser != null) {
            return RespBean.error(RespBeanEnum.MOBILE_HAS_REGISTERED);
        }
        User user = new User();
        user.setId(Long.valueOf(mobile));
        user.setNickname(registerVo.getNickname());
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        user.setSalt(salt);
        String dbPass = MD5Util.formPassToDBPass(registerVo.getPassword(), salt);
        user.setPassword(dbPass);
        user.setHead("https://example.com/default-avatar.png");
        user.setRegisterDate(new java.util.Date());
        userMapper.insert(user);
        return RespBean.success("注册成功");
    }
}