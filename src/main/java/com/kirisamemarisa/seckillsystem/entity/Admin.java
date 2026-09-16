package com.kirisamemarisa.seckillsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 管理员账号，对应表 {@code t_admin}。用户名唯一；密码列存 BCrypt，旧数据可能仍是无盐 MD5。
 */
@Data
@TableName("t_admin")
public class Admin implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;
    /** BCrypt（{@code $2} 开头）或遗留无盐 MD5；登录成功后会升级。 */
    private String password;
}
