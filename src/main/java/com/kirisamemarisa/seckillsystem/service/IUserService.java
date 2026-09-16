package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.vo.LoginVo;
import com.kirisamemarisa.seckillsystem.vo.RegisterVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;

/**
 * 普通用户业务。对应表 {@code t_user}，由 {@link com.kirisamemarisa.seckillsystem.service.impl.UserServiceImpl} 实现。
 * 手机号即主键；登录成功后 Token 缓存的是脱敏用户（不含 password/salt）。
 */
public interface IUserService extends IService<User> {
    /**
     * 用户登录。
     *
     * <p>手机号对应用户不存在或密码不匹配返回 {@code LOGIN_ERROR}；成功签发 Token 并更新 {@code last_login_date}。
     * 无 {@code @Transactional}：登录更新与可能发生的 BCrypt 升级都是单条 SQL。
     *
     * @param loginVo 手机号 + 密码（兼容明文或旧前端 MD5 表单密码）
     * @return 成功 {@code obj} 为 token；失败 {@link com.kirisamemarisa.seckillsystem.vo.RespBeanEnum#LOGIN_ERROR}
     */
    RespBean doLogin(LoginVo loginVo);

    /**
     * 用户注册。手机号已存在返回 {@code MOBILE_HAS_REGISTERED}。
     *
     * <p>新账号密码直接 BCrypt，{@code salt} 置 {@code null}。无显式事务，单条 {@code insert}。
     *
     * @param registerVo 昵称、手机号、明文密码
     * @return 成功提示文案；失败 {@link com.kirisamemarisa.seckillsystem.vo.RespBeanEnum#MOBILE_HAS_REGISTERED}
     */
    RespBean doRegister(RegisterVo registerVo);
}
