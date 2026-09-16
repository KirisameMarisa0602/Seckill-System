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

    @NotBlank(message = "管理员密码不能为空")
    private String password;
}
