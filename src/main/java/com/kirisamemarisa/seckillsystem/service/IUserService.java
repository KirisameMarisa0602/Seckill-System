package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.vo.LoginVo;
import com.kirisamemarisa.seckillsystem.vo.RegisterVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;

public interface IUserService extends IService<User> {
    //用户登录
    RespBean doLogin(LoginVo loginVo);
    //用户注册
    RespBean doRegister(RegisterVo registerVo);
}