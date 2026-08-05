package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class RegisterVo {
    @NotBlank(message = "昵称不能为空")
    @Length(min = 2, max = 20, message = "昵称长度必须在2到20个字符之间")
    private String nickname;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String mobile;

    @NotBlank(message = "密码不能为空")
    @Length(min = 32, message = "密码必须是MD5格式(长度32位)")
    private String password;
}