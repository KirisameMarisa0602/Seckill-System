/**
 * 数据库实体，与 Flyway 脚本 {@code db/migration} 中的表结构对应。
 * 使用 MyBatis-Plus 注解映射；雪花主键字段对前端序列化为字符串，避免 JavaScript 精度丢失。
 */
package com.kirisamemarisa.seckillsystem.entity;
