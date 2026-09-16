package com.kirisamemarisa.seckillsystem.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.kirisamemarisa.seckillsystem.entity.Admin;
import com.kirisamemarisa.seckillsystem.vo.AdminLoginVo;
import com.kirisamemarisa.seckillsystem.vo.RespBean;

/**
 * 管理员账号业务。对应表 {@code t_admin}，由 {@link com.kirisamemarisa.seckillsystem.service.impl.AdminServiceImpl} 实现。
 * 继承 {@code IService<Admin>} 后可走 MyBatis-Plus 通用 CRUD；本接口只额外暴露登录。
 */
public interface IAdminService extends IService<Admin> {
    /**
     * 管理员登录。
     *
     * <p>账号不存在或密码不匹配返回 {@code LOGIN_ERROR}；成功则签发 UUID Token 写入 Redis，
     * {@code obj} 为 token 字符串，前端放进 {@code Admin-Token} 请求头。
     * 无独立事务：校验通过后可能顺带把旧 MD5 密码升级为 BCrypt（单条 {@code updateById}）。
     *
     * @param vo 用户名 + 明文密码
     * @return 成功带 token；失败 {@link com.kirisamemarisa.seckillsystem.vo.RespBeanEnum#LOGIN_ERROR}
     */
    RespBean login(AdminLoginVo vo);
}
