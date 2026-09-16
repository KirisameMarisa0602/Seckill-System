package com.kirisamemarisa.seckillsystem.vo;

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
    private Long id;
    private String nickname;
    private String head;
    private LocalDateTime registerDate;
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
