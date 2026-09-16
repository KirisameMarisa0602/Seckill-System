package com.kirisamemarisa.seckillsystem.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kirisamemarisa.seckillsystem.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证用户实体序列化契约：密码与盐不得出现在 API JSON 中。
 */
class UserMapperTest {

    /**
     * 断言 {@code User.password}、{@code User.salt} 带 {@code @JsonIgnore}。
     */
    @Test
    void passwordAndSaltAreNeverSerializedToApiResponses() throws Exception {
        assertNotNull(User.class.getDeclaredField("password").getAnnotation(JsonIgnore.class));
        assertNotNull(User.class.getDeclaredField("salt").getAnnotation(JsonIgnore.class));
    }
}
