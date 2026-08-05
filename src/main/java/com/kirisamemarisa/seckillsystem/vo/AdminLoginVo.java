package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminLoginVo {
    @NotBlank(message = "管理员账号不能为空")
    private String username;

    @NotBlank(message = "管理员密码不能为空")
    private String password;
}