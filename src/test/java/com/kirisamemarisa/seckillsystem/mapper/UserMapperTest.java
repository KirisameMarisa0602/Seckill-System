package com.kirisamemarisa.seckillsystem.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kirisamemarisa.seckillsystem.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    @Test
    void passwordIsNeverSerializedToApiResponses() throws Exception {
        assertNotNull(User.class.getDeclaredField("password").getAnnotation(JsonIgnore.class));
    }
}
