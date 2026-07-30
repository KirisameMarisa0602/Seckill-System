package com.kirisamemarisa.seckillsystem.controller;

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

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    //看到参数前有@Valid注解，先把JSON映射为LoginVo对象检查参数，通过后再进入login方法，不通过直接在spring框架层面抛出异常
    public RespBean login(@Valid @RequestBody LoginVo loginVo) {
        return userService.doLogin(loginVo);
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public RespBean register(@Valid @RequestBody RegisterVo registerVo) {
        return userService.doRegister(registerVo);
    }
}