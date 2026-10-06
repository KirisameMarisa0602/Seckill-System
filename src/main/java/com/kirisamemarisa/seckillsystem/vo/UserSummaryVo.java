package com.kirisamemarisa.seckillsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
