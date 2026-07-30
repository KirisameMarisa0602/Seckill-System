package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kirisamemarisa.seckillsystem.entity.Admin;
import com.kirisamemarisa.seckillsystem.vo.AdminLoginVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;

public interface IAdminService extends IService<Admin> {
    // 真实查库登录逻辑
    RespBean login(AdminLoginVo vo);
}