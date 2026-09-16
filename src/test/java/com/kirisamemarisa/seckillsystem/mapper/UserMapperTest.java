package com.kirisamemarisa.seckillsystem.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kirisamemarisa.seckillsystem.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {
    @Test
    void passwordAndSaltAreNeverSerializedToApiResponses() throws Exception {
        assertNotNull(User.class.getDeclaredField("password").getAnnotation(JsonIgnore.class));
        assertNotNull(User.class.getDeclaredField("salt").getAnnotation(JsonIgnore.class));
    }
}
