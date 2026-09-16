package com.kirisamemarisa.seckillsystem.vo;

import com.kirisamemarisa.seckillsystem.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserSummaryVo {
    private Long id;
    private String nickname;
    private String head;
    private LocalDateTime registerDate;
    private LocalDateTime lastLoginDate;

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
