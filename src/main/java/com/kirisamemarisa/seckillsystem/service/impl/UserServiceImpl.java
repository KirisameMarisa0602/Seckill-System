package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.mapper.UserMapper;
import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.utils.MD5Util;
import com.kirisamemarisa.seckillsystem.vo.LoginVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RespBean doLogin(LoginVo loginVo) {
        String mobile = loginVo.getMobile();
        String pass = loginVo.getPassword();

        // 1. 根据手机号从数据库获取用户
        User user = userMapper.selectById(Long.valueOf(mobile));
        if (user == null) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        // 2. MD5 密码双重校验（前端发来的是一次加密密码，我们将它与数据库的随机盐结合判断）
        String calcPass = MD5Util.formPassToDBPass(pass, user.getSalt());
        if (!calcPass.equals(user.getPassword())) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        // 3. 密码正确，签发唯一 Token 作会话凭证
        String token = UUID.randomUUID().toString().replace("-", ""); // 去掉中划线

        // 4. 将用户信息序列化进 Redis 中（以此替代传统的 Tomcat Session），有效期设为30天
        // Key长这样： session:user:fa2c1...
        redisTemplate.opsForValue().set("session:user:" + token, user, 30, TimeUnit.DAYS);

        // 5. 登录成功，把 Token 返回给前端，前端后续请求都要带上这个 Token
        return RespBean.success(token);
    }
}