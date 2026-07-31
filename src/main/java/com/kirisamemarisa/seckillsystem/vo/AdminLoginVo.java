package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminLoginVo {
    @NotNull(message = "管理员账号不能为空")
    private String username;

    @NotNull(message = "管理员密码不能为空")
    private String password;
}