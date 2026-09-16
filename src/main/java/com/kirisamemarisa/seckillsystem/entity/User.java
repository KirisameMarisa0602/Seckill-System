package com.kirisamemarisa.seckillsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 普通用户，对应表 {@code t_user}。主键就是手机号（{@code IdType.INPUT}，非自增）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 手机号，注册时由业务写入，不是数据库自增。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String nickname;
    /** BCrypt 或遗留「随机盐 + 双次 MD5」哈希；序列化时忽略，避免进 Token/JSON。 */
    @JsonIgnore
    private String password;
    /**
     * 旧 MD5 方案的随机盐。新注册与升级后的账号为 {@code null}，只给 {@code MD5Util} 升级路径用。
     */
    @JsonIgnore
    private String salt;
    private String head;
    private LocalDateTime registerDate;
    private LocalDateTime lastLoginDate;
}
