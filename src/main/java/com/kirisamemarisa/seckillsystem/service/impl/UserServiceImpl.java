package com.kirisamemarisa.seckillsystem.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

/**
 * {@link IUserService} 实现。读写表 {@code t_user}。
 * 主键是手机号（{@code IdType.INPUT}），不是自增。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired private UserMapper userMapper;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private PasswordEncoder passwordEncoder;

    /**
     * 校验手机号与密码，签发 Token 并更新最近登录时间。
     *
     * @param loginVo 手机号 + 密码
     * @return 成功时 {@code obj} 为 token；失败 {@code LOGIN_ERROR}
     */
    @Override
    public RespBean doLogin(LoginVo loginVo) {
        String mobile = loginVo.getMobile();
        String pass = loginVo.getPassword();
        User user = userMapper.selectById(Long.valueOf(mobile));
        if (user == null || !passwordMatchesAndUpgrade(user, pass)) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }
        user.setLastLoginDate(LocalDateTime.now());
        userMapper.updateById(user);
        String token = UUID.randomUUID().toString().replace("-", "");
        // Redis 只放脱敏副本，避免 token 被盗后直接拿到 password/salt
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setNickname(user.getNickname());
        safeUser.setHead(user.getHead());
        safeUser.setRegisterDate(user.getRegisterDate());
        redisTemplate.opsForValue().set(UserKey.token.getPrefix() + token, safeUser, UserKey.token.expireSeconds(), TimeUnit.SECONDS);
        return RespBean.success(token);
    }

    /**
     * 按手机号注册。已存在则拒绝；新账号密码直接 BCrypt。
     *
     * @param registerVo 昵称、手机号、明文密码
     * @return 成功提示，或 {@code MOBILE_HAS_REGISTERED}
     */
    @Override
    public RespBean doRegister(RegisterVo registerVo) {
        String mobile = registerVo.getMobile();
        if (userMapper.selectById(Long.valueOf(mobile)) != null) { return RespBean.error(RespBeanEnum.MOBILE_HAS_REGISTERED); }
        User user = new User();
        user.setId(Long.valueOf(mobile));
        user.setNickname(registerVo.getNickname());
        user.setSalt(null);
        user.setPassword(passwordEncoder.encode(registerVo.getPassword()));
        user.setHead("https://api.dicebear.com/7.x/avataaars/svg?seed=" + mobile);
        user.setRegisterDate(LocalDateTime.now());
        userMapper.insert(user);
        return RespBean.success("注册成功");
    }

    /**
     * 校验密码并在命中旧 MD5 时升级为 BCrypt、清空 salt。
     *
     * <p>BCrypt 哈希以 {@code $2} 开头。旧账号同时认「前端已 MD5 的 formPass」和「明文再套静态盐」两种写法，
     * 避免升级窗口里有人用新前端、有人用旧前端登不进去。{@code salt.length() < 6} 是因为 MD5 混盐要用 charAt(0/2/5/4)。
     */
    private boolean passwordMatchesAndUpgrade(User user, String submittedPassword) {
        String stored = user.getPassword();
        if (stored != null && stored.startsWith("$2")) {
            return passwordEncoder.matches(submittedPassword, stored);
        }
        String salt = user.getSalt();
        if (stored == null || salt == null || salt.length() < 6) {
            return false;
        }
        boolean matched = MD5Util.formPassToDBPass(submittedPassword, salt).equals(stored)
                || MD5Util.inputPassToDBPass(submittedPassword, salt).equals(stored);
        if (matched) {
            user.setPassword(passwordEncoder.encode(submittedPassword));
            user.setSalt(null);
            userMapper.updateById(user);
        }
        return matched;
    }
}
