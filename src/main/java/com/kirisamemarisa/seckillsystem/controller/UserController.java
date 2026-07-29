package com.kirisamemarisa.seckillsystem.controller;

import com.kirisamemarisa.seckillsystem.service.IUserService;
import com.kirisamemarisa.seckillsystem.vo.LoginVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户前台控制中心 (登录、注册)
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 接收登录请求
     * @Valid 注解会自动触发 LoginVo 里设置的 @NotNull, @Pattern 等校验
     * 如果参数不合格，不进入方法体，直接抛错
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public RespBean login(@Valid @RequestBody LoginVo loginVo) {
        // 交给 Service 验证账号密码并发放 Token
        return userService.doLogin(loginVo);
    }
}