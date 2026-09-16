package com.kirisamemarisa.seckillsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kirisamemarisa.seckillsystem.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台用户列表摘要。从 {@link User} 拷贝公开字段，去掉 password/salt。
 */
@Data
@AllArgsConstructor
public class UserSummaryVo {
    /** 用户主键，即注册手机号。 */
    private Long id;
    /** 展示用昵称。 */
    private String nickname;
    /** 头像 URL。 */
    private String head;

    /** 注册时间，JSON 固定 {@code yyyy-MM-dd HH:mm:ss}。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerDate;

    /** 最近登录时间，未登录过可为空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginDate;

    /** 实体转摘要；调用方保证 {@code user} 非空。 */
    public static UserSummaryVo from(User user) {
        return new UserSummaryVo(
                user.getId(),
                user.getNickname(),
                user.getHead(),
                user.getRegisterDate(),
                user.getLastLoginDate()
        );
    }
}
