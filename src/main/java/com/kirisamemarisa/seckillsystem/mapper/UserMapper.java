package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.User;

/**
 * {@code t_user} 的 Mapper。主键即手机号，登录/注册用 {@code selectById(Long.valueOf(mobile))}。
 */
public interface UserMapper extends BaseMapper<User> {
}
