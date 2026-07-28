package com.kirisamemarisa.seckillsystem.vo;

import com.kirisamemarisa.seckillsystem.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀消息体，作为扔进 RabbitMQ 队列的数据载体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {
    private User user;
    private Long goodsId;
}