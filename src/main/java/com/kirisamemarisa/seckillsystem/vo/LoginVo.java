package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录入参。手机号即 {@code t_user.id}；密码 8~72 位以兼容 BCrypt 上限。
 */
@Data
public class LoginVo {
    /** 11 位手机号，同时作为 {@code t_user.id}。 */
    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String mobile;

    /** 明文密码；旧前端也可能先做一层 MD5 再提交。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度必须在8到72个字符之间")
    private String password;
}
