package com.kirisamemarisa.seckillsystem.mapper;

import com.kirisamemarisa.seckillsystem.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@SpringBootTest // 启动 Spring 上下文
@Transactional  // 测试结束后自动回滚，保持数据库整洁
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testInsertAndSelectUser() {
        // 1. 构建测试数据 (主键是手机号，使用 INPUT 策略)
        Long phoneId = 13800138001L;
        User user = User.builder()
                .id(phoneId)
                .nickname("Marisa")
                .password("b7797cce01b4b131b433b6acf4add449")
                .salt("1a2b3c4d")
                .registerDate(new Date())
                .lastLoginDate(new Date())
                .build();

        // 2. 测试插入
        int insertResult = userMapper.insert(user);
        Assertions.assertEquals(1, insertResult, "User 插入应该返回 1");

        // 3. 测试查询
        User queriedUser = userMapper.selectById(phoneId);
        Assertions.assertNotNull(queriedUser, "查询出来的 User 不应为空");
        Assertions.assertEquals("Marisa", queriedUser.getNickname(), "昵称应该匹配");
    }
}