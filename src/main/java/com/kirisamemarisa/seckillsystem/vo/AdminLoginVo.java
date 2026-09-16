package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录入参，对应 {@code t_admin.username / password}。
 */
@Data
public class AdminLoginVo {
    @NotBlank(message = "管理员账号不能为空")
    private String username;

    /** 明文密码；旧数据可能仍是无盐 MD5，登录成功后升级为 BCrypt。 */
    @NotBlank(message = "管理员密码不能为空")
    private String password;
}
