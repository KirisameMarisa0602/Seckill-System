/**
 * RabbitMQ 适配：秒杀下单消息发送、消费落库、Outbox 补偿发布以及死信队列超时关单。
 * 热路径先 Redis Lua 预扣库存并写入 Outbox，再由发布器投递 MQ，避免“扣了库存但消息丢失”。
 */
package com.kirisamemarisa.seckillsystem.rabbitmq;
