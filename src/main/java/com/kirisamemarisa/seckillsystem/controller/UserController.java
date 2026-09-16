package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.vo.LoginVo;
import com.kirisamemarisa.seckillsystem.vo.RegisterVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端用户认证入口，对应前端 {@code authApi}（LoginView / RegisterView）。
 *
 * <p>处于秒杀链路的身份前置：登录拿到的 {@code token} 写入请求头后，后续秒杀、下单、支付才能解析出 {@code User}。
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    /**
     * 用户登录，对应 {@code POST /user/login}、{@code authApi.login}。
     *
     * @param loginVo 手机号与密码；{@code @Valid} 校验失败不会进入本方法，由全局异常处理返回
     * @return 成功时 {@code obj} 为用户 token 字符串
     * @implNote 读用户表并更新最近登录时间；可能升级历史 MD5 密码为 BCrypt；token 对应的用户摘要写入 Redis
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @AccessLimit(second = 60, maxCount = 10, needLogin = false)
    public RespBean login(@Valid @RequestBody LoginVo loginVo) {
        return userService.doLogin(loginVo);
    }

    /**
     * 用户注册，对应 {@code POST /user/register}、{@code authApi.register}。
     *
     * @param registerVo 昵称、手机号、密码
     * @return 成功提示，或手机号已注册
     * @implNote 仅插入用户表，不写 Redis/MQ；注册后需再调登录获取 token
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @AccessLimit(second = 60, maxCount = 5, needLogin = false)
    public RespBean register(@Valid @RequestBody RegisterVo registerVo) {
        return userService.doRegister(registerVo);
    }
}
