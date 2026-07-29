package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class LoginVo {
    @NotNull(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String mobile;
    @NotNull(message = "密码不能为空")
    @Length(min = 32, message = "密码必须是MD5格式(长度32位)")
    private String password;
}