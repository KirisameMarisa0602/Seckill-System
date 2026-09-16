package com.kirisamemarisa.seckillsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 秒杀订单行，对应表 {@code t_seckill_order}。用 {@code (user_id, goods_id)} 唯一索引保证一人一单。
 * 超时关单会删除本行，从而释放再抢资格。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_seckill_order")
public class SeckillOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 雪花主键，对前端序列化为字符串。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private Long userId;

    /** 对应 {@code t_order.id}，同样是雪花 ID，序列化为字符串。 */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    private Long goodsId;
}
