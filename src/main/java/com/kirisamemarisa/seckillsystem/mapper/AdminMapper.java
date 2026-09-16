package com.kirisamemarisa.seckillsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kirisamemarisa.seckillsystem.entity.Admin;

/**
 * {@code t_admin} 的 MyBatis-Plus Mapper。登录按用户名查走 {@code QueryWrapper}，无自定义 SQL。
 */
public interface AdminMapper extends BaseMapper<Admin> {
}
